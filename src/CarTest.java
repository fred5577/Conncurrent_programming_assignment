//Prototype implementation of Car Test class
//Mandatory assignment
//Course 02158 Concurrent Programming, DTU, Fall 2017

//Hans Henrik Lovengreen     Oct 9, 2017

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
                    for (int j = 0; j < 1000; j++) {
                        cars.startCar(1);
                        cars.println("Start car 1");
                        sleep(10);
                        cars.setSpeed(1, 20);
                        sleep(750);
                        cars.setSpeed(1, 10000);
                        cars.stopCar(1);
                        cars.println("Car 1 blocks the alley");
                        for (int i = 5; i < 9; i++) {
                            cars.startCar(i);
                        }
                        sleep(10);
                        for (int i = 5; i < 9; i++) {
                            cars.setSpeed(i, 50);
                        }
                        sleep(5000);
                        cars.println("Remove and restore car 5 at once");
                        cars.removeCar(5);
                        cars.restoreCar(5);
                        sleep(5000);
                        cars.println("Remove car 7");
                        cars.removeCar(7);
                        cars.println("Start car 2 and 3");
                        cars.startCar(2);
                        cars.startCar(3);
                        sleep(10);
                        cars.setSpeed(2, 20);
                        cars.setSpeed(3, 20);
                        cars.stopCar(2);
                        cars.stopCar(3);
                        sleep(5000);
                        cars.println("Remove and restore all cars in the alley at once");
                        for (int i = 1; i < 4; i++) {
                            cars.removeCar(i);
                            cars.restoreCar(i);
                            //sleep(i*5);
                        }
                        sleep(10);
                        cars.removeCar(6);
                        sleep(100);
                        cars.restoreCar(6);
                        cars.restoreCar(7);
                        cars.setSpeed(1, 20);
                        cars.stopAll();
                        sleep(5000);
                    }
                    break;
            case 19:
                // Demonstration of speed setting.
                // Change speed to double of default values
                cars.println("Doubling speeds");
                for (int i = 1; i < 9; i++) {
                    cars.setSpeed(i,50);
                };
                break;

            default:
                cars.println("Test " + testno + " not available");
            }

            cars.println("Test ended");

        } catch (Exception e) {
            System.err.println("Exception in test: "+e);
        }
    }

}



