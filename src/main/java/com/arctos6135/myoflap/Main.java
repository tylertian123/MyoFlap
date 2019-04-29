package com.arctos6135.myoflap;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

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

        while(collector.getAccelerometerData() == null) {
            hub.runOnce(1000);
        }
        collector.setRefOrientation(collector.getOrientationQuat());

        long timeFlapHigh = 0;
        
        try {
            robot = new Robot();
        }
        catch(AWTException e) {
            System.err.println("ERROR: Cannot create Robot");
            System.exit(3);
        }

        while (true) {
            // Run the hub until one event occurs
            hub.runOnce(1000);
            double acceleration = collector.getAccelerometerData().z();
            long time = System.currentTimeMillis();
            // Factor in gravity
            if(acceleration < -0.5) {
                System.out.println("[" + System.currentTimeMillis() + "] High-to-Low Flap! (" + acceleration + ")");
                if(time - timeFlapHigh <= 3000) {
                    System.out.println("\u001b[4m\u001b[1mFlap!\u001b[0m");
                    robot.keyPress(KeyEvent.VK_SPACE);
                    robot.keyRelease(KeyEvent.VK_SPACE);
                }
                timeFlapHigh = 0;
            }
            else if(acceleration > 2.0) {
                System.out.println("[" + System.currentTimeMillis() + "] Low-to-High Flap! (" + acceleration + ")");
                timeFlapHigh = time;
            }
        }
    }
}