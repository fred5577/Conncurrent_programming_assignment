//Prototype implementation of Car Test class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2017

//Hans Henrik Lovengreen     Oct 9, 2017

import java.util.Random;

public class CarTest extends Thread {

    CarTestingI cars;
    int testno;

    public CarTest(CarTestingI ct, int no) {
        cars = ct;
        testno = no;
    }

    public void run() {
        try {
            switch (testno) {
                case 0:
                    // Demonstration of startAll/stopAll.
                    // Should let the cars go one round (unless very fast)
                    cars.startAll();
                    sleep(3000);
                    cars.stopAll();
                    break;
                case 1:
                    int speed = 20;
                    cars.startAll();
                    for (int i = 1; i < 9; i++) {
                        cars.setSpeed(i, speed);
                    }
                    for (int j = 0; j < 50; j++) {
                        sleep(500);
                        int rand = 1 + new Random().nextInt(8);
                        cars.removeCar(rand);
                        sleep(500);
                        cars.restoreCar(rand);
                        cars.setSpeed(rand, speed);
                    }
                    break;
                case 19:
                    // Demonstration of speed setting.
                    // Change speed to double of default values
                    cars.println("Doubling speeds");
                    for (int i = 1; i < 9; i++) {
                        cars.setSpeed(i, 50);
                    }
                    ;
                    break;

                default:
                    cars.println("Test " + testno + " not available");
            }

            cars.println("Test ended");

        } catch (Exception e) {
            System.err.println("Exception in test: " + e);
        }
    }

}



