package sk.lkce.techforms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class App {

	public static void main(String[] args) {

		JFrame frame = new JFrame();
		JPanel contentPane = new JPanel(new GridLayout(3, 2));

		JButton pickFileButton = new JButton("Choose .xls file");
		JLabel pickFileLbl = new JLabel("No input file chosen");
		pickFileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();
				pickFileLbl.setText(f.getAbsolutePath());
			}
		});

		JButton pickDirButton = new JButton("Choose output dir");
		JLabel pickDirLbl = new JLabel("No ouput dir chosen");
		pickDirButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.showOpenDialog(frame);
				File f = chooser.getSelectedFile();
				pickDirLbl.setText(f.getAbsolutePath());
			}
		});
		
		
		JButton goButton = new JButton("Generate reports");
		goButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		contentPane.add(pickFileButton);
		contentPane.add(pickFileLbl);
		contentPane.add(pickDirButton);
		contentPane.add(pickDirLbl);
		contentPane.add(goButton);

		JLabel statusLbl = new JLabel("........");

		JPanel bottomPane = new JPanel(new GridLayout(2, 1));
		JTextField warningsBox = new JTextField();
		bottomPane.add(statusLbl);
		bottomPane.add(warningsBox);
		frame.add(bottomPane, BorderLayout.SOUTH);

		int width = 50;
		contentPane.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

		frame.add(contentPane);
		frame.pack();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		frame.setVisible(true);
	}

}
