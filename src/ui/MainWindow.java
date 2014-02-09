package ui;

/*
 * Author: Ricardo "Mongskie" Benitez
 * Url: github.com/BoyKagud, facebook.com/emongb, plus.google.com/+MongskieBenitez/
 */

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class MainWindow extends JFrame {

	private JPanel contentPane;
	private DefaultUneditableModel model;
	private JScrollPane scrollPane;
	private JTable table;
	private Runtime rt;
	private JPanel main_wrap;
	private JPanel headerPane;
	private JPanel mkdirWrap;
	private JTextField textField;
	private LinkedList<String> breadCrumb;
	private LinkedList<String> history;
	private JLabel crumbLabel;
	private ProcessBuilder builder;
	private Process p;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainWindow frame = new MainWindow();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	public MainWindow() throws IOException {
		// Initialize history directory crumb //
		history = new LinkedList<String>();
		// Initialize directory bread crumb and point to C as root directory  //
		breadCrumb = new LinkedList<String>();
		breadCrumb.add("C:");
		// ================== //
		
		URL imageurl = getClass().getResource("/img/red.png");
        Image icon = Toolkit.getDefaultToolkit().getImage(imageurl);
        
		this.setIconImage(icon);
		this.setTitle("Xplorer-M");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(300, 200, 650, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		main_wrap = new JPanel();
		contentPane.add(main_wrap, BorderLayout.CENTER);
		main_wrap.setLayout(new BorderLayout(0, 0));
				
		scrollPane = new JScrollPane();
		main_wrap.add(scrollPane, BorderLayout.CENTER);
				
		setupTable();
		setUpHeader();
		populateList();
	}
	
	private void setupTable() {
		table = new JTable();
		table.setShowGrid(false);
		
		initializeModel();	

		table.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseReleased(java.awt.event.MouseEvent e) {
		        if (e.isPopupTrigger()) {
		        	
		            int r = table.rowAtPoint(e.getPoint());
		            table.setRowSelectionInterval(r, r);
		            
		        	PopUp menu = new PopUp();
			        menu.show(e.getComponent(), e.getX(), e.getY());
		            return;
		        }
		        
		        if(e.getClickCount() == 2) {
					try {
						String item = table.getValueAt(table.getSelectedRow(), 0).toString();
						if(isFolder())
							downOneLevel(item);
						else
							openFile(item);
					} catch (IOException e1) {e1.printStackTrace();	}
				}
		    }
			
			class PopUp extends JPopupMenu {
			    JMenuItem rename;
			    JMenuItem delete;
			    public PopUp(){
			    	rename = new JMenuItem("Rename");
				    delete = new JMenuItem("Delete");
			        add(rename);
			        add(delete);			        
			        delete.addMouseListener(new MouseAdapter() {
						@Override
					    public void mouseReleased(java.awt.event.MouseEvent e) {
							String item = table.getValueAt(table.getSelectedRow(), 0).toString();
							try {
								if(isFolder())
									delFolder("\""+item+"\"");
								else
									delFile("\""+item+"\"");
								executeCrumb();
							} catch (IOException e2) {}
						}
					});
			        rename.addMouseListener(new MouseAdapter() {
			        	@Override
					    public void mouseReleased(java.awt.event.MouseEvent e) {
			        		String name = table.getValueAt(table.getSelectedRow(), 0).toString();
			        		String newName = JOptionPane.showInputDialog("Enter new name");
			        		try {
								rename(name, newName);
				        		executeCrumb();
							} catch (IOException e1) {}
			        	}
					});
			    }
			}
			
		});
		scrollPane.setViewportView(table);
	}
	
	private void setUpHeader() throws IOException {
		headerPane = new JPanel();
		contentPane.add(headerPane, BorderLayout.NORTH);
		
		JPanel navWrap = new JPanel();

		JPanel back = new JPanelWithBgImage(new ImageIcon(this.getClass().getResource("/img/back.png")));
		back.setOpaque(false);
		back.setPreferredSize(new Dimension(40, 40));
		back.addMouseListener(new MouseAdapter() {
			@Override
		    public void mouseReleased(java.awt.event.MouseEvent e) {
				try {
					System.out.println("back");
					upOneLevel();
				} catch (IOException e1) {e1.printStackTrace();	}
		    }
		});
		navWrap.add(back);
		
		JPanel next = new JPanelWithBgImage(new ImageIcon(this.getClass().getResource("/img/next.png")));
		next.setOpaque(false);
		next.setPreferredSize(new Dimension(30, 30));
		next.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(java.awt.event.MouseEvent e) {
				breadCrumb.add(history.removeLast());
				try {
					executeCrumb();
				} catch (IOException e1) {}
			}
		});
		headerPane.setLayout(new BorderLayout(0, 0));
		navWrap.add(next);
		
		crumbLabel = new JLabel(getCrumb());
		navWrap.add(crumbLabel);

		headerPane.add(navWrap, BorderLayout.WEST);
		
		mkdirWrap = new JPanel();
		headerPane.add(mkdirWrap, BorderLayout.EAST);
		
		textField = new JTextField();
		mkdirWrap.add(textField);
		textField.setColumns(20);

		JPanel mkdir = new JPanelWithBgImage(new ImageIcon(this.getClass().getResource("/img/add_folder.png")));
		mkdir.setOpaque(false);
		mkdir.setPreferredSize(new Dimension(40, 40));
		mkdir.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(java.awt.event.MouseEvent e) {
				String newDir = textField.getText();
				String cmd = "mkdir "+getCrumb()+newDir;
				if(!newDir.equals("")) {
					try {
						executeCmd(cmd);
						executeCrumb();
						textField.setText("");
					} catch (IOException e1) {}
				} else {
					JOptionPane.showMessageDialog(null, "Folder name required");
				}
			}
		});
		mkdirWrap.add(mkdir);
	}
	
	private void initializeModel() {
		model = new DefaultUneditableModel(
			new Object[][] {
			},
			new String[] {
				"Name", "Type", "Date Modified"
			}
		);
		table.setModel(model);
	}
	
	private void populateList() throws IOException {     
		executeCrumb();
	}
	
	private void downOneLevel(String folder) throws IOException {
		breadCrumb.add(folder);
		history = new LinkedList<String>();
		executeCrumb();
	}
	
	private void upOneLevel() throws IOException {
		history.add(breadCrumb.removeLast());
		executeCrumb();
	}
	
	private void executeCrumb() throws IOException {
		crumbLabel.setText(getCrumb().replaceAll("\"", ""));
		initializeModel();
		rt = Runtime.getRuntime();
		boolean disks = false;
		
		String crumb = getCrumb();		
		String cmd = "";
		if(!getCrumb().equals("")) 
			cmd = crumb.substring(0, 2)+" && cd "+getCrumb()+" && dir";
		else {
			cmd = "wmic logicaldisk get description, name";
			disks = true;
		}
		
		System.out.println(cmd);
		
		executeCmd(cmd);
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
		readTerminal(r, disks);
	}
	
	private void rename(String name, String newName) throws IOException {
		String[] temp = name.replace(".", " ").split(" ");
		String extension = "";
		if(!isFolder())
			extension = "."+temp[temp.length-1];
		executeCmd("REN "+getCrumb()+"\""+name+"\" \""+newName+extension+"\"");
	}
	
	private void openFile(String name) throws IOException {
		executeCmd("start "+getCrumb()+"\""+name+"\"");
	}
	
	public boolean isFolder() {
		String cell = table.getValueAt(table.getSelectedRow(), 1).toString();
		if(cell.equals("folder") || cell.contains("Dis"))
			return true;
		return false;
	}
	
	private void delFolder(String folder) throws IOException {
		String cmd = "rmdir "+getCrumb()+folder+" /s /q";
		executeCmd(cmd);
	}
	
	private void delFile(String file) throws IOException {
		String cmd = "del "+getCrumb()+file;
		executeCmd(cmd);
	}
	
	private void executeCmd(String cmd) throws IOException {
		builder = new ProcessBuilder(
	            "cmd.exe", "/c", cmd);
        builder.redirectErrorStream(true);
        p = builder.start();
        System.out.println(cmd);
	}
	
	private String getCrumb() {
		String res = "";
		int tmp = 0;
		for(String crumb : breadCrumb) {
			if(tmp==0)
				res += crumb+"\\";
			else
				res += "\""+crumb+"\"\\";
			tmp++;
		}
		return res;
	}
	
	private void readTerminal(BufferedReader stdin, boolean disks) throws IOException {
        String s = null;
        
        if(!disks) {
	        while ((s = stdin.readLine()) != null) {
	        	s = s.trim().replaceAll(" +", " ");
	        	String row[] = s.split("\\s");
	        	
	        	if(row.length < 5 || row[4].equals(".") || row[4].equals("..") || row[0].charAt(0) == 'V')
	        		continue;
	        	
	        	System.out.println(s);
	        	String type = getType(row);
	            model.addRow(new Object[] {getFilename(row), type, row[0]+" "+row[1]+row[2]});
	        }
	        //clean up
	        model.removeRow(model.getRowCount()-1);
        } else {
        	stdin.readLine();
	        while ((s = stdin.readLine()) != null) {
	        	s = s.trim().replaceAll(" +", " ");
	        	String row[] = s.split("\\s");
	        	
	        	if(row.length < 2)
	        		continue;
	        	
	        	System.out.println(s);
	        	String type = "";
	        	
	        	for(int it = 0; it < row.length-1 ; it++) {
	        		type += row[it]+" ";
	        	}
	        	
	        	model.addRow(new Object[] {row[row.length-1], type.substring(0, type.length()-1), "*********"});
	        }
        }
	}
	
	private String getType(String[] row) {
		if(row[3].equals("<DIR>"))
			return "folder";
		
		String[] temp = row[row.length-1].replace(".", " ").split(" ");
		return temp[temp.length-1].toUpperCase()+" File";
	}
	
	private String getFilename(String[] row) {
		if(row.length<5)
			return row[4];
		String name = "";
		for(int k = 4 ; k<row.length ; k++) {
			name += " "+row[k];
		}
		return name.substring(1, name.length());
	}
	
	
	/*================= SUBCLASSES ===================*/
	
	/* This class extends DefaultTableModel class which adds
	 * disabled editing on cells feature.
	 */
	public class DefaultUneditableModel extends DefaultTableModel {
		
		public DefaultUneditableModel(Object[][] o, String[] s) {
			super(o, s);
		}
		/*
		 * For both booleans below: by default, these variables
		 * are accessed on double click action on table cells.
		 * By manually setting it to false, no cells can be edited
		 */
		public boolean[] columnEditables = new boolean[] {
				false
		};
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	}

	/* This class is used for 
	 * images mainly on header 
	 */
	class JPanelWithBgImage extends JPanel {
		private static final long serialVersionUID = 1L;
		private ImageIcon bgImage;
	 
		public JPanelWithBgImage(ImageIcon ii) {
	         super();
	         this.bgImage = ii;
	         this.setCursor(new Cursor(Cursor.HAND_CURSOR));
	         setOpaque(true);
	      }

		public void paintComponent(Graphics g) {
	         super.paintComponent(g);
	         if (bgImage != null) {
	            Dimension size = this.getSize();
	            g.drawImage(bgImage.getImage(), 0,0, size.width, size.height, this);
	         }
	      }
	   }

}
