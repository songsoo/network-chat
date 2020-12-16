import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class client extends JFrame implements ActionListener, KeyListener {

	private Socket socket;
	private String IP;
	private int port;
	private InputStream in;
	private OutputStream out;
	private DataInputStream din;
	private DataOutputStream dout;
	private String nickname;

	Vector user_list = new Vector();
	StringTokenizer st;

	private JFrame Login;
	private JTextField IDText = new JTextField();
	JButton chatStart = new JButton("Start");
	KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

	private JFrame frame;
	JButton sendBtn = new JButton("Send");
	JButton quitBtn = new JButton("Quit");
	JScrollPane scrollPane_1 = new JScrollPane();
	JScrollPane scrollPane = new JScrollPane();
	JTextArea messages = new JTextArea();
	private final JScrollPane scrollPane_2 = new JScrollPane();
	private final JList list = new JList();
	private final JTextField sendingText_1 = new JTextField();

	/*
	 * 메인함수힙니다. 시작시 메인GUI를 보이지 않게 설정합니다.
	 */
	public static void main(String[] args) {

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					client window = new client();
					window.frame.setVisible(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*
	 * 네트워크를 설정하는 함수입니다. configuration 클래스를 통해 읽어온 IP와 port번호에 맞는 소켓을 생성하고, 정상적으로
	 * 생성되면 connection함수를 실행합니다.
	 */
	private void network() {
		try {
			socket = new Socket(IP, port);
			if (socket != null) {
				connection();
			}
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/**
	 * 네트워크 연결이후 설정하는 함수입니다. 소켓에 맞게 output,input stream들을 생성하고 로그인 GUI를 보이지않게 하고
	 * 메인GUI를 보이게 설정합니다. 이후 서버에게 자신의 닉네임을 전송하고, 서버에게서 이름을 새로 수정받습니다. 유저리스트에 모두에게
	 * 발송하는 ToALL을 추가합니다. 이후 반복문을 통해 서버로부터 문장을 읽어오고, 그것을 inMessage함수로 넣어 처리합니다.
	 */
	private void connection() {

		try {
			out = socket.getOutputStream();
			in = socket.getInputStream();
			dout = new DataOutputStream(out);
			din = new DataInputStream(in);
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "연결 실패", "알림", JOptionPane.ERROR_MESSAGE);
		}

		sendMessage(nickname);

		user_list.add("To ALL");
		Thread thr = new Thread(new Runnable() {
			public void run() {
				if (!nickname.equals("ss")){
					change();
					messages.append("채팅은 자신을 들여다보는 거울입니다. 매너 채팅부탁드립니다\n/는 입력시 문제가 발생할 수 있습니다.\n");
					while (true) {
						try {
							String msg = din.readUTF();
							inMessage(msg);
						} catch (IOException e1) {
							try {
								in.close();
								out.close();
								din.close();
								dout.close();
								socket.close();
								JOptionPane.showMessageDialog(null, "서버와 접속 끊어짐", "알림", JOptionPane.ERROR_MESSAGE);
								break;
							} catch (IOException e2) {
							}
						}

					}
				}

			}
		});

		thr.start();

	}

	/*
	 * 메시지의 프토로콜을 확인하고 그에 맞게 행동하는 함수입니다. 읽어온 문자열을 StringTokenizer를 통해 나누고 맨 처음 글자인
	 * Protocol을 본 뒤 행동을 취합니다 1. NewUser일 경우 새로운 유저가 접속했음을 채팅창에 띄우고 유저 리스트에 추가합니다.
	 * 2. OldUser일 경우 유저 리스트에 추가합니다. 3. MSG일 경우 새로운 토큰을 입력받고 2번째토큰(이름):
	 * 3번째토큰(메시지내용)을 채팅창에 띄웁니다. 4. Whisper To일 경우 새로운 토큰을 입력받고 2번째토큰(이름)>>자신의 이름
	 * 3번째토큰(메시지내용)을 채팅창에 띄웁니다. 5. Whisper From일 경우 새로운 토큰을 입력받고 자신의 이름 >>2번째토큰(이름)
	 * 3번째토큰(메시지내용)을 채팅창에 띄웁니다. 6. UserOut일 경우 유저 리스트에서 제거하고 퇴장메시지를 띄웁니다. 7. Set일 경우
	 * 선택목록에서 0을 선택합니다 (To ALL)
	 */
	private void inMessage(String str) {

		st = new StringTokenizer(str, "/");

		String protocol = st.nextToken();
		String message = st.nextToken();
		String msg;

		if (protocol.equals("NewUser")) {
			user_list.add(message);
			messages.append(message + "님이 새로 접속했습니다.\n");
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("OldUser")) {
			user_list.add(message);
		} else if (protocol.equals("MSG")) {
			String user = message;
			msg = st.nextToken();
			messages.append(user + ": " + msg);
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("WhisperTo")) {
			String user = message;
			msg = st.nextToken();
			messages.append(user + ">>" + nickname + ": " + msg);
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("WhisperFrom")) {
			String user = message;
			msg = st.nextToken();
			messages.append(nickname + ">>" + user + ": " + msg);
			scrollPane_1.getVerticalScrollBar().setValue(scrollPane_1.getVerticalScrollBar().getMaximum());
		} else if (protocol.equals("UserOut")) {
			user_list.remove(message);
			messages.append(message + "님이 퇴장하셨습니다.\n");
		} else if (protocol.equals("ListUpdate")) {
			list.setListData(user_list);
		} else if (protocol.equals("Set")) {
			list.setSelectedIndex(0);
		}

	}

	/*
	 * 메시지를 전송하는 함수입니다. 받은 문자열을 지정된 DataOutputStream으로 내보냅니다.
	 */
	private void sendMessage(String str) {
		try {
			dout.writeUTF(str);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "전송 실패", "알림", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

	/*
	 * 클래스 생성자 입니다. configuration 클래스를 생성하고 IP주소와 port번호를 읽어옵니다. 이후 GUI를 활성화 시키고
	 * start함수를 실행합니다.
	 */
	public client() throws FileNotFoundException, IOException {
		configuration Info = new configuration();
		IP = Info.getIP();
		port = Info.getPort();
		LoginInit();
		MainInit();
		start();
	}

	/*
	 * Login GUI구성입니다.
	 */
	private void LoginInit() {

		Login = new JFrame();
		Login.setBounds(100, 100, 450, 300);
		Login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Login.getContentPane().setLayout(null);

		IDText = new JTextField();
		IDText.setFont(new Font("굴림", Font.PLAIN, 30));
		IDText.setBounds(204, 74, 218, 42);
		Login.getContentPane().add(IDText);
		IDText.setColumns(10);

		chatStart.setFont(new Font("굴림", Font.BOLD, 30));
		chatStart.setBounds(151, 182, 116, 39);
		Login.getContentPane().add(chatStart);

		JLabel lblNewLabel = new JLabel("Nick Name");
		lblNewLabel.setFont(new Font("굴림", Font.BOLD, 30));
		lblNewLabel.setBounds(12, 74, 193, 45);
		Login.getContentPane().add(lblNewLabel);

		Login.setLocationRelativeTo(null);

	}

	/*
	 * Main GUI구성입니다.
	 */
	private void MainInit() {

		sendingText_1.setFont(new Font("굴림", Font.BOLD, 16));
		sendingText_1.setColumns(10);

		frame = new JFrame();
		frame.setBounds(100, 100, 840, 840 / 12 * 9);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);

		sendBtn.setFont(new Font("굴림", Font.BOLD, 14));
		sendBtn.setBounds(687, 439, 114, 61);
		frame.getContentPane().add(sendBtn);

		quitBtn.setFont(new Font("굴림", Font.BOLD, 14));
		quitBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		quitBtn.setBounds(687, 520, 114, 61);
		frame.getContentPane().add(quitBtn);

		scrollPane_1.setBounds(24, 10, 775, 416);
		frame.getContentPane().add(scrollPane_1);
		messages.setLineWrap(true);
		messages.setFont(new Font("Monospaced", Font.BOLD, 16));
		messages.setEditable(false);
		scrollPane_1.setViewportView(messages);
		scrollPane_2.setBounds(142, 439, 525, 142);

		frame.getContentPane().add(scrollPane_2);

		scrollPane_2.setViewportView(sendingText_1);

		list.setFont(new Font("굴림", Font.BOLD, 16));
		list.setBounds(24, 439, 106, 134);
		list.setListData(user_list);

		frame.getContentPane().add(list);

		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Login.setVisible(true);
		Login.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setVisible(false);
	}

	/*
	 * GUI 구성요소들의 ActionListener를 추가하는 함수입니다.
	 */
	private void start() {
		chatStart.addActionListener(this);
		sendBtn.addActionListener(this);
		quitBtn.addActionListener(this);
		sendingText_1.addKeyListener(this);
	}

	/*
	 * ActionEvent가 발생했을 때 처리하는 함수입니다. chatStart 버튼이 눌렸을 경우 이름의 길이가 0이 아니거나, null이
	 * 아니면 network를 실행합니다. sendBtn이 눌렸을 경우, 문자의 길이가 0이 아니고, 대상이 선택되지 않은 경우를 제외하고
	 * 메시지를 보냅니다. 메시지는 받는사람이름/보내는사람이름/문자내용 으로 구성되어있으며 전송이후 텍스트창을 비웁니다. quitBtn이 눌렸을
	 * 경우, 소켓을 닫고 시스템을 종료합니다.
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == chatStart) {
			nickname = IDText.getText();
			if (IDText.getText().length() == 0 || IDText.getText().equals("null")) {
				IDText.requestFocus();
			} else {
				network();
			}
		} else if (e.getSource() == sendBtn && sendingText_1.getText().length() != 0
				&& !list.getSelectedValue().equals("null")) {
			String msg = sendingText_1.getText();
			String Receiver = (String) list.getSelectedValue();
			String Sender = this.nickname;
			sendMessage(Receiver + "/" + Sender + "/" + msg);
			sendingText_1.setText(null);
			sendingText_1.requestFocus();
		} else if (e.getSource() == quitBtn) {
			try {
				socket.close();
				System.exit(0);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

		}

	}

	public void keyPressed(KeyEvent arg0) {

	}

	/*
	 * 키보드가 눌렸을 때 처리하는 함수입니다. actionPerformed함수에서 sendBtn을 눌렀을 경우와 동일하게 메시지를 전송합니다.
	 */
	public void keyReleased(KeyEvent arg0) {
		if (arg0.getKeyCode() == KeyEvent.VK_ENTER && sendingText_1.getText().length() != 0
				&& !list.getSelectedValue().equals("null")) {
			String msg = sendingText_1.getText();
			String Receiver = (String) list.getSelectedValue();
			String Sender = this.nickname;
			sendMessage(Receiver + "/" + Sender + "/" + msg);
			sendingText_1.setText(null);
			sendingText_1.requestFocus();
		}
	}

	public void keyTyped(KeyEvent arg0) {

	}

	public void change() {
		this.frame.setVisible(true);
		this.Login.setVisible(false);
	}

}
