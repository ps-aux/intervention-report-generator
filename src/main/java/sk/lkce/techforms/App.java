package sk.lkce.techforms;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import javax.swing.SwingWorker;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

public class App implements ReportGeneratorListener {

	private static final String WIN_TITLE = "Intervention Report Generator";
	private static final String APP_TITLE = "Intervention Report Generator 1.2";

	private static final Border BORDER = BorderFactory.createEmptyBorder(10,
			10, 10, 10);

	private static final Border BORDER_TOP_BOTTOM = BorderFactory
			.createEmptyBorder(10, 0, 5, 0);

	private String inputFile;
	private String outputDir;
	private JTextArea msgBox;
	private JLabel statusLbl;
	private JFrame frame;
	private JButton goButton, parseButton;
	private ReportGenerator generator;

	public App() {
		
		inputFile = System.getProperty("repgen.input");
		outputDir = System.getProperty("repgen.output");
		
		
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
		String pfLblTxt =	inputFile == null ? "No input file chosen" : inputFile;
		final JLabel pickFileLbl = new JLabel(pfLblTxt);

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
				inputFile = f.getAbsolutePath();
				checkButtons();
			}
		});

		JButton pickDirButton = new JButton("Choose output dir");
		String pdLblTxt =	inputFile == null ? "No output dir chosen" : outputDir;
		final JLabel pickDirLbl = new JLabel(pdLblTxt);
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
				outputDir = f.getAbsolutePath();
				checkButtons();
			}
		});

		goButton = new JButton("Generate reports");
		goButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				start();
			}
		});

		parseButton = new JButton("Parse excel table");
		parseButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				statusLbl.setText("Parsing record table");
				parseButton.setEnabled(false);
				goButton.setEnabled(false);

				SwingWorker<List<Record>, Void> worker = new SwingWorker<List<Record>, Void>() {

					Exception e;

					@Override
					protected List<Record> doInBackground() throws Exception {
						try {

							generator = new ReportGenerator(inputFile,
									outputDir, App.this);
							List<Record> recs = generator.parseRecords();
							return recs;
						} catch (Exception e) {
							this.e = e;
							throw e;
						}

					}

					@Override
					protected void done() {

						parseButton.setEnabled(true);
						goButton.setEnabled(true);
						if (e != null)
							reportError(e);

						statusLbl.setText("Record table parsed");

						List<Record> recs;
						try {
							recs = get();
							new RecordTable(frame, recs,
									new SelectedRecordsCallback()).showDialog();
						} catch (InterruptedException | ExecutionException e) {
							reportError(e);
						}

					}

				};

				worker.execute();
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
		contentPane.add(Box.createVerticalStrut(10));
		contentPane.add(parseButton);

		statusLbl = new JLabel();
		statusLbl.setBorder(BORDER_TOP_BOTTOM);

		JPanel bottomPane = new JPanel();
		bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.PAGE_AXIS));

		msgBox = new JTextArea();
		msgBox.setRows(15);
		msgBox.setEditable(false);
		bottomPane.add(statusLbl);
		bottomPane.add(new JScrollPane(msgBox));

		checkButtons();

		bottomPane.setBorder(BORDER);

		frame.add(bottomPane, BorderLayout.SOUTH);
		frame.add(contentPane);
		frame.setSize(650, 600);
		frame.setLocationRelativeTo(null);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.setResizable(false);

		frame.setVisible(true);
		
	}
	
	private void checkButtons (){
		boolean enabled =  inputFile != null && outputDir != null;
		parseButton.setEnabled(enabled);
		goButton.setEnabled(enabled);

	}

	private void reportError(final Exception e) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						String localMsg = "Error happended during the generation of reports:\n"
								+ e.getMessage();
						e.printStackTrace();
						JOptionPane.showMessageDialog(frame, localMsg,
								"Something went wrong",
								JOptionPane.ERROR_MESSAGE);
					}
				});
			}
		});

	}

	private void addMsg(final String msg) {

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
		final ReportGenerator generator = new ReportGenerator(inputFile, outputDir,
				this);

		// No need for swing worker here.
		Runnable ru = new Runnable() {

			@Override
			public void run() {

				try {
					generator.parseAndProcessAll();
				} catch (Exception e) {
					reportError(e);
				}
				executionFinished();
			}
		};

		goButton.setEnabled(false);
		parseButton.setEnabled(false);
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
				parseButton.setEnabled(true);
			}
		});
	}

	@Override
	public void warningIssued(String msg) {
		addMsg("WARNING: " + msg);
	}

	@Override
	public void messageIssued(String msg) {
		addMsg(msg);
	}

	@Override
	public void statusMsgChanged(final String msg) {

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

	private class SelectedRecordsCallback implements GenerateForRecordsCallback {

		@Override
		public void generateForRecords(final List<Record> records) {

			assert generator != null;
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				Exception e;

				@Override
				protected Void doInBackground() throws Exception {
					try {
						generator.processRecords(records);
					} catch (Exception e) {
						this.e = e;
						throw e;
					}
					
					return null;
				}

				@Override
				protected void done() {

					executionFinished();
					if (e != null)
						reportError(e);
				}

			};

			worker.execute();
		}
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
