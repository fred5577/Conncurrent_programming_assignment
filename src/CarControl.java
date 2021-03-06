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

    boolean V_new = false;
    boolean V_current = false;

    boolean inAlley = false;
    Semaphore deleteSem = new Semaphore(1);


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
            this.speed = speed;
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

                if (curpos.equals(barpos)) {
                    CarControl.barrier.sync();
                }

                newpos = nextPos(curpos);
                CarControl.alley.enter(no);

                CarControl.semaphores[newpos.row][newpos.col].P();

                deleteSem.P();
                cd.clear(curpos);
                cd.mark(curpos, newpos, col, no);
                V_new = true;
                deleteSem.V();

                sleep(speed());

                deleteSem.P();
                cd.clear(curpos, newpos);
                cd.mark(newpos, col, no);
                CarControl.alley.leave(no);
                int tR = curpos.row;
                int tC = curpos.col;
                curpos = newpos;
                CarControl.semaphores[tR][tC].V();
                V_new = false;
                deleteSem.V();
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

    static int counterU = 0;
    static int counterD = 0;

    synchronized void countDown(int no) {
        if (CarControl.car[no].inAlley) {
            if (no > 4) {
                counterU--;
            } else {
                counterD--;
            }
        }
    }

    synchronized void enterDirection(int no) throws InterruptedException {
        if (no < 5) {
            while (counterU > 0) {
                wait();
            }
        } else {
            while (counterD > 0) {
                wait();
            }
        }
        CarControl.car[no].deleteSem.P();
        if (no < 5) {
            counterD++;
        } else {
            counterU++;
        }
        CarControl.car[no].inAlley = true;
        CarControl.car[no].deleteSem.V();
    }

    synchronized void leaveDirection(String direction) {
        if (direction.equals("Down")) {
            counterD--;
        } else {
            counterU--;
        }
        if (counterD + counterU == 0) {
            notifyAll();
        }
    }

    public synchronized void enter(int no) throws InterruptedException {
        if (no < 5) {
            if ((CarControl.car[no].newpos.row == 2 && CarControl.car[no].newpos.col == 1) || (CarControl.car[no].newpos.row == 1 && CarControl.car[no].newpos.col == 2)) {
                enterDirection(no);
            }
        } else if (CarControl.car[no].newpos.row == 9 && CarControl.car[no].newpos.col == 0) {
            enterDirection(no);
        }
    }

    public synchronized void leave(int no) {
        if ((no < 5 && CarControl.car[no].newpos.row == 9 && CarControl.car[no].newpos.col == 1) || (CarControl.car[no].newpos.row == 0 && CarControl.car[no].newpos.col == 2)) {
            if (no < 5) {
                leaveDirection("Down");
            } else {
                leaveDirection("Up");
            }
            CarControl.car[no].inAlley = false;
        }
    }

}

class Barrier {

    private int count = 0;

    private boolean on = false;
    private boolean shutDownOn = false;
    private boolean allExit = true;

    public synchronized void sync() throws InterruptedException {
        if (!on) {
            return;
        }
        count++;
        //System.out.println(shutDownOn);

        if (count != 9) {
            allExit = false;
            while (!allExit) {
                wait();
            }
        }

        // Critical region, her passere vi barrieren

        count--;
        allExit = true;
        notifyAll();
        if (shutDownOn && count == 0) {
            off();
        }
    }

    public synchronized void on() throws InterruptedException {
        if (!on) {
            count = 0;
            on = true;
        }
    }

    public synchronized void off() throws InterruptedException {
        on = false;
        shutDownOn = false;
        allExit = true;
        notifyAll();
    }

    public synchronized void shutDown() throws InterruptedException {
        if (!on) {
            return;
        } else if (on && count == 0) {
            return;
        }
        shutDownOn = true;
        wait();
    }
}

public class CarControl implements CarControlI {


    public static boolean[] removed = new boolean[9];
    public static Semaphore timeSem = new Semaphore(9);
    public static Semaphore[][] semaphores = new Semaphore[11][12];
    CarDisplayI cd;           // Reference to GUI
    public static Car[] car;               // Cars
    Gate[] gate;              // Gates
    public static Barrier barrier = new Barrier();
    public static Alley alley = new Alley();

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
        try {
            barrier.on();
        } catch (InterruptedException e) {
        }
//        cd.println("Barrier On not implemented in this version");
    }

    public void barrierOff() {
        try {
            barrier.off();
        } catch (InterruptedException e) {
        }
        //cd.println("Barrier Off not implemented in this version");
    }

    public void barrierShutDown() {
        // This sleep is for illustrating how blocking affects the GUI
        // Remove when shutdown is implemented.
        try {
            barrier.shutDown();
        } catch (InterruptedException e) {
        }
        // Recommendation: 
        //   If not implemented call barrier.off() instead to make graphics consistent
    }

    public void setLimit(int k) {
        cd.println("Setting of bridge limit not implemented in this version");
    }

    public void removeCar(int no) {
        try {
            if (!removed[no]) {
                car[no].deleteSem.P();
                car[no].interrupt();
                removed[no] = true;
                alley.countDown(no);
                if (car[no].V_new) {
                    cd.clear(car[no].newpos, car[no].curpos);
                    semaphores[car[no].newpos.row][car[no].newpos.col].V();
                    semaphores[car[no].curpos.row][car[no].curpos.col].V();
                } else {
                    cd.clear(car[no].curpos);
                    semaphores[car[no].curpos.row][car[no].curpos.col].V();
                }
                car[no].deleteSem.V();
                notifyAll();
            }
        } catch (Exception e) {
        }
        cd.println("Remove Car " + no);
    }


    public void restoreCar(int no) {
        if (removed[no]) {
            removed[no] = false;
            car[no] = new Car(no, cd, gate[no]);
            car[no].start();
        }
        cd.println("Restore Car " + no);
    }

    /* Speed settings for testing purposes */

    public void setSpeed(int no, int speed) {
        car[no].setSpeed(speed);
    }

    public void setVariation(int no, int var) {
        car[no].setVariation(var);
    }

}






