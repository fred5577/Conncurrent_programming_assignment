//Prototype implementation of Car Control
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2017

//Hans Henrik Lovengreen     Oct 9, 2017


import java.awt.Color;

class Gate {

    Semaphore g = new Semaphore(0);
    Semaphore e = new Semaphore(1);
    boolean isopen = false;

    public void pass() throws InterruptedException {
        g.P();
        g.V();
    }

    public void open() {
        try {
            e.P();
        } catch (InterruptedException e) {
        }
        if (!isopen) {
            g.V();
            isopen = true;
        }
        e.V();
    }

    public void close() {
        try {
            e.P();
        } catch (InterruptedException e) {
        }
        if (isopen) {
            try {
                g.P();
            } catch (InterruptedException e) {
            }
            isopen = false;
        }
        e.V();
    }

}

class Car extends Thread {

    int basespeed = 100;             // Rather: degree of slowness
    int variation = 50;             // Percentage of base speed

    CarDisplayI cd;                  // GUI part

    int no;                          // Car number
    Pos startpos;                    // Startpositon (provided by GUI)
    Pos barpos;                      // Barrierpositon (provided by GUI)
    Color col;                       // Car  color
    Gate mygate;                     // Gate at startposition


    int speed;                       // Current car speed
    Pos curpos;                      // Current position 
    Pos newpos;                      // New position to go to

    public Car(int no, CarDisplayI cd, Gate g) {

        this.no = no;
        this.cd = cd;
        mygate = g;
        startpos = cd.getStartPos(no);
        try {
            CarControl.semaphores[startpos.row][startpos.col].P();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        barpos = cd.getBarrierPos(no);  // For later use

        col = chooseColor();

        // do not change the special settings for car no. 0
        if (no == 0) {
            basespeed = 0;
            variation = 0;
            setPriority(Thread.MAX_PRIORITY);
        }
    }

    public synchronized void setSpeed(int speed) {
        if (no != 0 && speed >= 0) {
            basespeed = speed;
        } else
            cd.println("Illegal speed settings");
    }

    public synchronized void setVariation(int var) {
        if (no != 0 && 0 <= var && var <= 100) {
            variation = var;
        } else
            cd.println("Illegal variation settings");
    }

    synchronized int chooseSpeed() {
        double factor = (1.0D + (Math.random() - 0.5D) * 2 * variation / 100);
        return (int) Math.round(factor * basespeed);
    }

    private int speed() {
        // Slow down if requested
        final int slowfactor = 3;
        return speed * (cd.isSlow(curpos) ? slowfactor : 1);
    }

    Color chooseColor() {
        return Color.blue; // You can get any color, as longs as it's blue 
    }

    Pos nextPos(Pos pos) {
        // Get my track from display
        return cd.nextPos(no, pos);
    }

    boolean atGate(Pos pos) {
        return pos.equals(startpos);
    }

    public void run() {
        try {

            speed = chooseSpeed();
            curpos = startpos;
            cd.mark(curpos, col, no);

            while (true) {
                sleep(speed());

                if (atGate(curpos)) {
                    mygate.pass();
                    speed = chooseSpeed();
                }
                newpos = nextPos(curpos);
                Alley.enter(no);
                CarControl.semaphores[newpos.row][newpos.col].P();

                //  Move to new position
                cd.clear(curpos);
                cd.mark(curpos, newpos, col, no);
                sleep(speed());
                cd.clear(curpos, newpos);
                cd.mark(newpos, col, no);
                Alley.leave(no);
                CarControl.semaphores[curpos.row][curpos.col].V();
                curpos = newpos;
            }

        } catch (Exception e) {
            cd.println("Exception in Car no. " + no);
            System.err.println("Exception in Car no. " + no + ":" + e);
            e.printStackTrace();
        }
    }

}

class Alley {

    /***
     * Inspired by chapter 4, page 170, section 4.4.2 readers/writers.
     * Since the example are used multiple readers who are excluded from the writer, we thought about creating two groups of "readers" each group excluded from the other group.
     */

    static Semaphore down = new Semaphore(1);
    static Semaphore up = new Semaphore(1);
    static Semaphore read = new Semaphore(1);

    static int counterU = 0;
    static int counterD = 0;


    static void enterDirection(String direction) {
        try {
            if (direction.equals("Down")) {
                down.P();
                counterD++;
                if (counterD == 1) {
                    read.P();
                }
                down.V();
            } else {
                up.P();
                counterU++;
                if (counterU == 1) {
                    read.P();
                }
                up.V();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void leaveDirection(String direction) {
        try {
            if (direction.equals("Down")) {
                down.P();
                counterD--;
                if (counterD == 0) {
                    read.V();
                }
                down.V();
            } else {
                up.P();
                counterU--;
                if (counterU == 0) {
                    read.V();
                }
                up.V();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void enter(int no) {
        if (no < 5) {
            if ((CarControl.car[no].newpos.row == 2 && CarControl.car[no].newpos.col == 1) || (CarControl.car[no].newpos.row == 1 && CarControl.car[no].newpos.col == 2)) {
                enterDirection("Down");
            }
        } else if (CarControl.car[no].newpos.row == 9 && CarControl.car[no].newpos.col == 0) {
            enterDirection("Up");
        }
    }

    static void leave(int no) {
        if ((no < 5 && CarControl.car[no].newpos.row == 9 && CarControl.car[no].newpos.col == 1) || (CarControl.car[no].newpos.row == 0 && CarControl.car[no].newpos.col == 2)) {
            if (no < 5) {
                leaveDirection("Down");
            } else {
                leaveDirection("Up");
            }
        }
    }
}

class Barrier {

    static Semaphore on = new Semaphore(1);

    public void sync(){

    }

    public void on(){

    }

    public void off(){

    }
}


public class CarControl implements CarControlI {

    public static Barrier barrier;

    public static Semaphore[][] semaphores = new Semaphore[11][12];
    CarDisplayI cd;           // Reference to GUI
    public static Car[] car;               // Cars
    Gate[] gate;              // Gates

    public CarControl(CarDisplayI cd) {
        this.cd = cd;
        car = new Car[9];
        gate = new Gate[9];
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 12; j++) {
                semaphores[i][j] = new Semaphore(1);
            }
        }

        for (int no = 0; no < 9; no++) {
            gate[no] = new Gate();
            car[no] = new Car(no, cd, gate[no]);
            car[no].start();
        }
    }

    public void startCar(int no) {
        gate[no].open();
    }

    public void stopCar(int no) {
        gate[no].close();
    }

    public void barrierOn() {
        cd.println("Barrier On not implemented in this version");
        barrier.on();
    }

    public void barrierOff() {
        cd.println("Barrier Off not implemented in this version");
        barrier.off();
    }

    public void barrierShutDown() {
        cd.println("Barrier shut down not implemented in this version");
        // This sleep is for illustrating how blocking affects the GUI
        // Remove when shutdown is implemented.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }
        // Recommendation: 
        //   If not implemented call barrier.off() instead to make graphics consistent
    }

    public void setLimit(int k) {
        cd.println("Setting of bridge limit not implemented in this version");
    }

    public void removeCar(int no) {
        cd.println("Remove Car not implemented in this version");
    }

    public void restoreCar(int no) {
        cd.println("Restore Car not implemented in this version");
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) {
        car[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) {
        car[no].setVariation(var);
    }

}






