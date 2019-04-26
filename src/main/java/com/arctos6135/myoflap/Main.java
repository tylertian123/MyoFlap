package com.arctos6135.myoflap;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;

import com.arctos6135.myoflap.myo.DataCollector;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.MyoException;

public class Main {

    public static Hub hub;
    public static Myo myo;
    public static DataCollector collector = new DataCollector();

    public static Robot robot;

    public static void main(String[] args) {

        // Make sure native resources are released
        // Add a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (myo != null) {
                    myo.lock();
                }
                if (hub != null) {
                    hub.release();
                }
            }
        });

        // Initialize the Myo
        try {
            System.out.println("******* MyoFlap *******");
            System.out.println("Constructing Hub...");
            // Create the hub
            hub = new Hub("com.arctos6135.MyoFlap");
            System.out.println("Waiting for Myo...");
            // Wait for the Myo for a maximum of 10 seconds
            myo = hub.waitForMyo(10000);
            if (myo == null) {
                System.err.println("ERROR: Wait for Myo timed out");
                System.exit(2);
            }
            System.out.println("Initializing Myo...");
            hub.addListener(collector);
            // Use standard locking policy for now since we only need the IMU data
            hub.setLockingPolicy(Hub.LockingPolicy.lockingPolicyStandard);
            System.out.println("Initialization complete.");
        } catch (MyoException e) {
            System.err.println("ERROR: Cannot connect to the Myo");
            System.exit(1);
        }

        while(collector.getOrientationQuat() == null) {
            hub.runOnce(1000);
        }
        collector.setRefOrientation(collector.getOrientationQuat());

        long timeFlapLow = 0;
        long timeFlapHigh = 0;
        int counter = 0;
        
        try {
            robot = new Robot();
        }
        catch(AWTException e) {
            System.err.println("ERROR: Cannot create Robot");
            System.exit(3);
        }

        double mid = -13;
        double highest = Double.NEGATIVE_INFINITY;
        double lowest = Double.POSITIVE_INFINITY;

        while (true) {
            // Run the hub until one event occurs
            hub.runOnce(1000);
            // Only do stuff if the Myo is actually on the arm
            if (collector.onArm()) {
                double pitch = collector.getOrientationEuler().getPitchDegrees();
                
                //System.out.println("Pitch: " + pitch);
                if(pitch > highest) {
                    highest = pitch;
                }
                if(pitch < lowest) {
                    lowest = pitch;
                }
                long time = System.currentTimeMillis();
                if(pitch > mid + 1) {
                    if(time - timeFlapHigh <= 3000) {
                        System.out.println("\u001b[4m\u001b[1mFlap!\u001b[0m");
                        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                        //mid = (lowest + highest) / 2;
                        highest = Double.NEGATIVE_INFINITY;
                        lowest = Double.POSITIVE_INFINITY;
                    }
                    timeFlapHigh = 0;
                    timeFlapLow = time;
                }
                else if(pitch < mid - 1) {
                    if(timeFlapHigh == 0 && time - timeFlapLow <= 3000) {
                        timeFlapHigh = time;
                    }
                }

                if(counter++ > 10) {
                    System.out.printf("Pitch: %.2f\tBounds: %.2f\t%.2f\nTimes: %d\t%d\n", pitch, mid + 1, mid - 1,
                            timeFlapLow, timeFlapHigh);
                }
            }
        }
    }
}