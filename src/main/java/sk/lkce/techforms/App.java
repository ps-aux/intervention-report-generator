package sk.lkce.techforms;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

public class App implements ReportGeneratorListener {

	private static final String WIN_TITLE = "Intervention Report Generator";
	private static final String APP_TITLE = "Intervention Report Generator 1.0";

	private static final Border BORDER = BorderFactory.createEmptyBorder(10,
			10, 10, 10);

	private static final Border BORDER_TOP_BOTTOM = BorderFactory
			.createEmptyBorder(10, 0, 5, 0);

	private String sourceFile = "/home/luce/Downloads/input.xlsx";
	private String targetDir = "/home/luce/tmp/makro/";
	private JTextArea msgBox;
	private JLabel statusLbl;
	private JFrame frame;
	private JButton goButton;

	public App() {
		frame = new JFrame();
		frame.setTitle(WIN_TITLE);

		JLabel titleLbl = new JLabel(APP_TITLE);
		titleLbl.setHorizontalAlignment(SwingConstants.CENTER);
		titleLbl.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

		frame.add(titleLbl, BorderLayout.NORTH);

		JPanel contentPane = new JPanel();
		BoxLayout layout = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
		contentPane.setLayout(layout);

		JButton pickFileButton = new JButton("Choose input   file");
		JLabel pickFileLbl = new JLabel("No input file chosen");
		pickFileButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				if (JFileChooser.APPROVE_OPTION != chooser
						.showOpenDialog(frame))
					return;
				File f = chooser.getSelectedFile();
				pickFileLbl.setText(f.getAbsolutePath());
				sourceFile = f.getAbsolutePath();
			}
		});

		JButton pickDirButton = new JButton("Choose output dir");
		JLabel pickDirLbl = new JLabel("No ouput dir chosen");
		pickDirButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				if (JFileChooser.APPROVE_OPTION != chooser
						.showOpenDialog(frame))
					return;
				File f = chooser.getSelectedFile();
				pickDirLbl.setText(f.getAbsolutePath());
				targetDir = f.getAbsolutePath();
			}
		});

		goButton = new JButton("Generate reports");
		goButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});

		contentPane.setBorder(BORDER);

		pickFileLbl.setBorder(BORDER_TOP_BOTTOM);
		contentPane.add(pickFileLbl);
		contentPane.add(pickFileButton);

		pickDirLbl.setBorder(BORDER_TOP_BOTTOM);
		contentPane.add(pickDirLbl);
		contentPane.add(pickDirButton);

		contentPane.add(Box.createVerticalStrut(30));
		contentPane.add(goButton);

		statusLbl = new JLabel("Doing something for sure bro");
		statusLbl.setBorder(BORDER_TOP_BOTTOM);

		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.PAGE_AXIS));

		msgBox = new JTextArea();
		msgBox.setRows(15);
		msgBox.setEditable(false);
		bottomPane.add(statusLbl);
		bottomPane.add(new JScrollPane(msgBox));

		bottomPane.setBorder(BORDER);

		frame.add(bottomPane, BorderLayout.SOUTH);
		frame.add(contentPane);
		frame.setSize(600, 520);
		frame.setLocationRelativeTo(null);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);

		frame.setVisible(true);

	}

	private void reportError(String msg) {
		msg = "Error happended during the generation of reports:\n" + msg;
		JOptionPane.showMessageDialog(frame, msg, "Something went wrong",
				JOptionPane.ERROR_MESSAGE);

	}

	private void addMsg(String msg) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						if (msgBox.getText().isEmpty())
							msgBox.setText(msg);
						else
							msgBox.setText(msgBox.getText() + "\n" + msg);
					}
				});
			}
		});

	}

	private void start() {
		ReportGenerator generator = new ReportGenerator(sourceFile, targetDir,
				this);

		// No need for swing worker here.
		Runnable ru = new Runnable() {

			@Override
			public void run() {

				try {
					generator.go();
				} catch (Exception e) {
					reportError(e.getMessage());
				}
				executionFinished();
			}
		};

		goButton.setEnabled(false);
		new Thread(ru).start();
	}

	public static void main(String[] args) {
		setLookAndFeel();
		new App();
	}

	private void executionFinished() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				goButton.setEnabled(true);
			}
		});
	}

	@Override
	public void warningIssued(String msg) {
		System.out.println("warning " + msg);
		addMsg(msg);
	}

	@Override
	public void messageIssued(String msg) {
		System.out.println("msg " + msg);
		addMsg(msg);
	}

	@Override
	public void statusMsgChanged(String msg) {

		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				statusLbl.setText(msg);
			}
		});
	}

	@Override
	public void generationFinished(String msg) {
		// Nothing
	}

	private static void setLookAndFeel() {
		try {

			String lookAndFeel = javax.swing.UIManager
					.getSystemLookAndFeelClassName();

			if (lookAndFeel.endsWith("MetalLookAndFeel")) // This might be Linux
															// so let's try GTK
															// look and feel.
				lookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";

			javax.swing.UIManager.setLookAndFeel(lookAndFeel);

			javax.swing.UIManager.getDefaults().put("Button.showMnemonics",
					Boolean.TRUE);
		} catch (ClassNotFoundException e) {
			// Nothing to do here. Just print to err stream.
			e.printStackTrace();
		} catch (InstantiationException e) {
			// Nothing to do here. Just print to err stream.
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// Nothing to do here. Just print to err stream.
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// Nothing to do here. Just print to err stream.
			e.printStackTrace();
		}
	}

}
