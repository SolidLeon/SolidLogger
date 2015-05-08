package com.solidleon.solidlogger.logging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class Logging {

	private static SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	private static Scanner sc = new Scanner(System.in);
	private static JFrame logWindow = null;
	private static JPanel logPanel = null;
	private static JTextPane text = null;
	
	private static enum LogLevel {
		INFO(Color.black, Color.white),
		WARNING(Color.yellow.darker(), Color.white),
		ERROR(Color.red.darker(), Color.white),
		CRITICAL(Color.white, Color.red.darker());
		
		public Color fg = Color.white;
		public Color bg = Color.black;
		
		private LogLevel(Color fg, Color bg) {
			this.fg = fg;
			this.bg = bg;
		}

		private LogLevel() {
			this(Color.black, Color.white);
		}
				
	}
	
	private static void log(LogLevel level, String format, Object ...args) {
		String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
		String prefix = String.format("%-10s - %s", Thread.currentThread().getName(), caller);
		String str = String.format("%s %-8s: %-50s %s%n", 
				sdf.format(new Date()), 
				level, 
				prefix, 
				args == null || args.length == 0 ? format : String.format(format, args));
		if (logPanel == null)
			System.out.printf(str);
		else
			appendText(level.fg, level.bg, str);
		if (level == LogLevel.CRITICAL) {
			System.exit(99);
		}
	}
	
	public static void info(String format, Object ...args) {
		log(LogLevel.INFO, format, args);
	}
	
	public static void error(String format, Object ...args) {
		log(LogLevel.ERROR, format, args);
	}
	
	public static void warning(String format, Object ...args) {
		log(LogLevel.WARNING, format, args);
	}

	public static void exception(Throwable e, String format, Object ...args) {
		log(LogLevel.ERROR, "Exception caught %s: '%s'", e.getClass().getName(), e.getMessage());
		if (format != null)
			log(LogLevel.ERROR, format, args);
		Path logDir = Paths.get("logs").toAbsolutePath();
		try {
			Files.createDirectory(logDir);
		} catch (FileAlreadyExistsException ex) {
			// Ignore
		} catch (IOException e2) {
			e2.printStackTrace();
			Logging.error(e2.getMessage());
		}
		Path path = logDir.resolve(UUID.randomUUID().toString() + ".txt").toAbsolutePath();
		Logging.error("Write stack trace to '%s'", path.toString());
		try (PrintStream ps = new PrintStream(Files.newOutputStream(path, StandardOpenOption.CREATE))) {
			e.printStackTrace(ps);
		} catch (IOException e1) {
			Logging.error(e1.getMessage());
			e1.printStackTrace();
		}
	}
	public static void exception(Throwable e) {
		exception(e, null);
	}
	
	public static void openLogWindow(boolean closeOnExit) {
		if (logWindow == null) {
			logWindow = new JFrame("Log Window");
			logWindow.setPreferredSize(new Dimension(640, 480));
			logWindow.setDefaultCloseOperation(closeOnExit ? JFrame.EXIT_ON_CLOSE : JFrame.HIDE_ON_CLOSE);
			
			logWindow.add(getLogPanel());
			
			
			logWindow.pack();
			logWindow.setLocationRelativeTo(null);
		}
		SwingUtilities.invokeLater(() -> logWindow.setVisible(true));
	}
	
	public static JPanel getLogPanel() {
		if (logPanel == null) {
			logPanel = new JPanel(new BorderLayout());
			text = new JTextPane() {
				@Override
				public boolean getScrollableTracksViewportWidth() {
					return getUI().getPreferredSize(this).width <= getParent().getSize().width;
				}
			};
			text.setEditable(false);
			text.setFont(new Font("Monospaced", Font.PLAIN, 12));
			DefaultCaret caret = (DefaultCaret)text.getCaret();
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			logPanel.add(new JScrollPane(text));
		}
		return logPanel;
	}

	private static void appendText(Color fg, Color bg, String str) {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				SimpleAttributeSet aset = new SimpleAttributeSet();
				StyleConstants.setForeground(aset, fg);
				StyleConstants.setBackground(aset, bg);
				
				int start = text.getDocument().getLength();
				int len = text.getText().length();
				StyledDocument sd = (StyledDocument) text.getDocument();
				sd.setCharacterAttributes(start, len, aset, false);
				

				try {
					text.getDocument().insertString(text.getDocument().getLength(), str, aset);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	
	public static void closeLogWindow() {
		logWindow.dispose();
	}
	
	public static JFrame getLogWindow() {
		return logWindow;
	}

	public static String readLine() {
		String result = null;
		if (logPanel != null) {
			result = JOptionPane.showInputDialog(logPanel, "Input:");
		} else {
			result = sc.nextLine();
		}
		return result;
	}
}
