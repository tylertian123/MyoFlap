package com.arctos6135.myoflap;

import com.arctos6135.myoflap.myo.DataCollector;
import com.arctos6135.myoflap.myo.EulerOrientation;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.MyoException;

public class Main {

    public static Hub hub;
    public static Myo myo;
    public static DataCollector collector = new DataCollector();

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

        while (true) {
            // // Run the hub until one event occurs
            // hub.runOnce(1000);
            // Run the hub for 100ms
            // Used during testing to not update the display too fast
            hub.run(100);
            // Only do stuff if the Myo is actually on the arm
            if (collector.onArm()) {
                EulerOrientation orientation = collector.getOrientationEuler();

                System.out.printf("Yaw=%.3f\tPitch=%.3f\tRoll=%.3f\n", orientation.getYawDegrees(),
                        orientation.getPitchDegrees(), orientation.getRollDegrees());
            }
            else {
                System.out.println("Myo is not on an arm right now.");
            }
        }
    }
}