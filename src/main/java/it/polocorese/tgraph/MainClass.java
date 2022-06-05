/**
 * Copyright (c) 2022 Nicolò Cantori
 *
 * This file is part of tgraph.
 * SPDX-License-Identifier: MIT
 */

package it.polocorese.tgraph;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;

public class MainClass {
    static SerialPort chosenPort;
    static int x = 0, number;
    static float V_i, temperature;

    public static void main(String[] args)
    {

        // Create the main window:

        JFrame window = new JFrame();
        window.setTitle("tgraph");
        window.setSize(600, 400);
        window.setLayout(new BorderLayout());
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a drop-down menu and a "connect" button:

        JComboBox<String> portList = new JComboBox<String>();
        JButton connectBtn = new JButton("Connect");

        // Place these elements at the top of the window:

        JPanel topPanel = new JPanel();
        topPanel.add(portList);
        topPanel.add(connectBtn);
        window.add(topPanel, BorderLayout.NORTH);

        // Scan for available serial ports, then populate the drop-down menu:

        SerialPort[] portNames = SerialPort.getCommPorts();
        for(int i = 0; i < portNames.length; i++)
            portList.addItem(portNames[i].getSystemPortName());

        // Create the line graph:

        XYSeries series = new XYSeries("Readings");
        XYSeriesCollection data = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Temperature sensor readings",
                "Time (seconds)",
                "Temperature (\u00B0" + "C)",
                data);

        window.add(new ChartPanel(chart), BorderLayout.CENTER);

        // Configure the "connect" button and use another thread to listen for incoming data:

        connectBtn.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if(connectBtn.getText().equals("Connect")) {

                    // Try to connect to the serial port:
                    chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
                    chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

                    if(chosenPort.openPort())
                    {
                        connectBtn.setText("Disconnect");
                        portList.setEnabled(false);
                    }

                    // Create a new thread that listens for incoming data and then populate the chart:
                    Thread thread = new Thread(){

                        @Override
                        public void run() {
                            Scanner scanner = new Scanner(chosenPort.getInputStream());

                            while(scanner.hasNextLine()) {
                                try {
                                    String line = scanner.nextLine();
                                    number = Integer.parseInt(line);

                                    V_i = (float) (number * 0.00488); // Arduino UNO ADC

                                    /**
                                     * The LM35 temperature sensor has a linear scale factor of
                                     * +10mV/°C
                                     *
                                     * @see <a href="https://www.ti.com/lit/gpn/lm35"> LM35
                                     * Datasheet</a>
                                     */
                                    temperature = V_i * 100;

                                    series.add(x++, temperature);
                                    window.repaint();
                                } catch(Exception e) {}
                            }
                            scanner.close();
                        }
                    };
                    thread.start();
                }
                else {

                    // Disconnect from the serial port:

                    chosenPort.closePort();

                    portList.setEnabled(true);
                    connectBtn.setText("Connect");
                    series.clear();
                    x = 0;
                }
            }
        });
        // Display the window:
        window.setVisible(true);
    }
}