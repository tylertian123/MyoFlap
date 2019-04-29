package com.arctos6135.myoflap;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.arctos6135.myoflap.myo.DataCollector;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.MyoException;

public class Main {

    public static Hub hub;
    public static Myo myo;
    public static DataCollector collector = new DataCollector();

    public static Robot robot;
    public static FlapProcessingThread flapProcessingThread;

    public static JFrame mainFrame;
    public static JPanel mainPanel;
    public static JButton onOffButton;
    public static JLabel onOffLabel;

    public static final String ON_TEXT = "ON";
    public static final String OFF_TEXT = "OFF";
    public static final Color ON_COLOR = Color.GREEN;
    public static final Color OFF_COLOR = Color.RED;
    
    public static boolean flappingOn = false;

    public static class FlapProcessingThread extends Thread {
        @Override
        public void run() {
            System.out.println("[Flap Processing Thread] Waiting for initial accelerometer data...");
            while(collector.getAccelerometerData() == null) {
                hub.runOnce(1000);
            }

            long timeFlapHigh = 0;
            System.out.println("[Flap Processing Thread] Creating Robot...");
            try {
                robot = new Robot();
            }
            catch(AWTException e) {
                System.err.println("ERROR: Cannot create Robot");
                System.exit(3);
            }
            System.out.println("[Flap Processing Thread] Entering main loop...");
            while (true) {
                if(flappingOn) {
                    // Run the hub until one event occurs
                    hub.runOnce(1000);
                    double acceleration = collector.getAccelerometerData().z();
                    long time = System.currentTimeMillis();
                    // Factor in gravity
                    if(acceleration < -0.5) {
                        System.out.println("[Flap Processing Thread] [" + System.currentTimeMillis() + "] High-to-Low Flap! (" + acceleration + ")");
                        if(time - timeFlapHigh <= 3000) {
                            System.out.println("[Flap Processing Thread] \u001b[4m\u001b[1mFlap!\u001b[0m");
                            robot.keyPress(KeyEvent.VK_SPACE);
                            robot.keyRelease(KeyEvent.VK_SPACE);
                        }
                        timeFlapHigh = 0;
                    }
                    else if(acceleration > 2.0) {
                        System.out.println("[Flap Processing Thread] [" + System.currentTimeMillis() + "] Low-to-High Flap! (" + acceleration + ")");
                        timeFlapHigh = time;
                    }
                }
                else {
                    System.out.println("[Flap Processing Thread] Flapping has been turned OFF! Suspending thread...");
                    try {
                        synchronized(this) {
                            wait();
                        }
                    }
                    catch(InterruptedException e) {
                        // Ignore
                        e.printStackTrace();
                    }
                    System.out.println("[Flap Processing Thread] Flapping has been turned back ON.");
                }
            }
        }
    }

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
            System.out.println("[Main Thread] Constructing Hub...");
            // Create the hub
            hub = new Hub("com.arctos6135.MyoFlap");
            System.out.println("[Main Thread] Waiting for Myo...");
            // Wait for the Myo for a maximum of 10 seconds
            myo = hub.waitForMyo(10000);
            if (myo == null) {
                System.err.println("[Main Thread] ERROR: Wait for Myo timed out");
                System.exit(2);
            }
            System.out.println("[Main Thread] Initializing Myo...");
            hub.addListener(collector);
            // Use standard locking policy for now since we only need the IMU data
            hub.setLockingPolicy(Hub.LockingPolicy.lockingPolicyStandard);
            System.out.println("[Main Thread] Initialization complete.");
        } catch (MyoException e) {
            System.err.println("[Main Thread] ERROR: Cannot connect to the Myo");
            System.exit(1);
        }
        
        System.out.println("[Main Thread] Constructing UI...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        onOffLabel = new JLabel(flappingOn ? ON_TEXT : OFF_TEXT);
        onOffLabel.setFont(onOffLabel.getFont().deriveFont(Font.BOLD));
        onOffLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        onOffLabel.setForeground(flappingOn ? ON_COLOR : OFF_COLOR);
        mainPanel.add(onOffLabel);

        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        onOffButton = new JButton("On/Off");
        onOffButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        onOffButton.addActionListener(e -> {
            flappingOn = !flappingOn;
            onOffLabel.setText(flappingOn ? ON_TEXT : OFF_TEXT);
            onOffLabel.setForeground(flappingOn ? ON_COLOR : OFF_COLOR);

            if(flappingOn) {
                synchronized(flapProcessingThread) {
                    flapProcessingThread.notifyAll();
                }
            }

            System.out.println("[Main Thread] Flapping has been turned " + (flappingOn ? "ON" : "OFF"));
        });
        mainPanel.add(onOffButton);

        mainFrame = new JFrame(); 
        mainFrame.setContentPane(mainPanel);
        mainFrame.pack();
        mainFrame.setName("MyoFlap");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setVisible(true);
        
        System.out.println("[Main Thread] Starting Flap Processing Thread...");
        flapProcessingThread = new FlapProcessingThread();
        flapProcessingThread.setDaemon(true);
        flapProcessingThread.start();
    }
}