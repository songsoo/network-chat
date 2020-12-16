import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.LinkedList;

/**
 * 유저 정보를 저장하기 위한 LinkedList user를 생성합니다. 이후 멀티쓰레드를 설정하고 연결을 받기 위한 ServerSocekt인
 * listener를 생성합니다 새로운 연결이 들어오면 멀티쓰레드를 통해 연결시킵니다.
 */
public class Server {

	private static LinkedList<UserInfo> users = new LinkedList<UserInfo>();

	public static void main(String[] args) throws Exception {
		System.out.println("The chat server is running...");
		ExecutorService pool = Executors.newFixedThreadPool(500);
		try (ServerSocket listener = new ServerSocket(9468)) {
			while (true) {
				pool.execute(new Handler(listener.accept()));
			}
		}
	}

	/**
	 * 클라이언트를 처리하는 클래스입니다.
	 */
	private static class Handler implements Runnable {
		private String name;
		private Socket socket;
		private InputStream in;
		private OutputStream out;
		private DataInputStream din;
		private DataOutputStream dout;

		/**
		 * Listener를 통해 새로 들어온 클라이언트를 Handler클래스의 소켓과 연결합니다.
		 */
		public Handler(Socket socket) {
			this.socket = socket;
		}

		/**
		 * 데이터 송수신을 위한 Input,Output Stream들을 생성하고, 이름을 읽어옵니다. 읽어온 이름을 중복이 있다면 뒤에 괄호를 통해
		 * 중복된 이름임을 표시하고 클라이언트에게 이 이름을 사용하라는 의미로 메시지를 보냅니다. 이후 이 이름을 프로토콜에 맞게 서버에 연결된 모든
		 * 클라이언트들에게 broadcast를 합니다 이후 새로운 유저 정보를 생성하고, 다른 유저정보를 프로토콜에 맞게 전송한 후에 연결리스트에
		 * 추가합니다. 추가한 이후 클라이언트들에게 유저 리스트를 업데이트 하라는 내용을 broadcast하고, 새로운 유저에게는 유저 리스트를
		 * ToAll에 놓도록 메시지를 전송합니다. 이후 반복적으로 클라이언트에게서 문장을 읽어오고 그에 맞는 행동을 inMessage함수를 통해
		 * 취합니다. 연결이 끊어질 경우 유저정보에서 제거하고 유저들에게 해당 유저가 나갔음을 알리는 메시지를 broadcast하며, 소켓을
		 * 닫습니다.
		 */
		public void run() {
			try {
				boolean duplicate = false;

				in = socket.getInputStream();
				din = new DataInputStream(in);
				out = socket.getOutputStream();
				dout = new DataOutputStream(out);

				name = din.readUTF();
				if (!name.equals("ss")) {

					int i = 1;
					while (checkName(name)) {
						if (i == 1) {
							name = name + "(" + i + ")";
						} else {
							String str = "(" + Integer.toString(i - 1) + ")";
							name = name.replace(str, "(" + i + ")");
						}
						i++;
					}
					broadcast("NewUser/" + name);

					UserInfo user = new UserInfo(dout, name);

					for (UserInfo curuser : users) {
						dout.writeUTF("OldUser/" + curuser.getName());
					}

					users.add(user);

					broadcast("ListUpdate/new");
					dout.writeUTF("Set/e");

					while (true) {
						String input = din.readUTF();
						inMessage(input);
					}
				}
				else {}

			} catch (

			Exception e) {
				System.out.println(e);
			} finally {
				int i = 0;
				for (UserInfo user : users) {
					if (user.getName().equals(name) || user.getStream() == dout) {
						break;
					}
					i++;
				}
				if (dout != null) {
					users.remove(i);
				} else if (name != null) {
					try {
						broadcast("UserOut/" + name);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					users.remove(i);
				}
				for (UserInfo user : users) {
					try {
						user.getStream().writeUTF("UserOut/" + name);
						user.getStream().writeUTF("ListUpdate/quit");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				try {
					socket.close();
				} catch (IOException e) {
				}
			}

		}

		private boolean checkName(String str) {
			for (UserInfo user : users) {
				if (user.getName().equals(str)) {
					return true;
				}
			}
			return false;
		}

		/**
		 * 읽어온 문자열을 프로토콜에 맞게 처리하는 함수입니다. 문자열을 StringTokenizer를 통해 세 개로 나누고, 첫번째는 받는 사람,
		 * 두번째는 보내는 사람, 세번째는 메시지로 저장합니다. 받는 사람이 ToALL일 경우 메시지를 프로토콜에 맞게 모든 유저에게
		 * broadcast합니다. 받는 사람이 지정되어있을 경우, 받는 사람과 보내는 사람이 같지 않다면, 프로토콜에 맞게 받는 사람, 보내는
		 * 사람에게 문자열을 보냅니다.
		 */
		private void inMessage(String str) throws IOException {
			// TODO Auto-generated method stub
			StringTokenizer st = new StringTokenizer(str, "/");

			String Receiver = st.nextToken();
			String Sender = st.nextToken();
			String Message = st.nextToken();

			if (Receiver.equals("To ALL")) {
				broadcast("MSG/" + Sender + "/" + Message + "\n");
			} else {
				if (!Receiver.equals(Sender)) {
					for (UserInfo user : users) {
						if (user.getName().equals(Receiver)) {
							user.getStream().writeUTF("WhisperTo/" + Sender + "/" + Message + "\n");
						} else if (user.getName().equals(Sender)) {
							user.getStream().writeUTF("WhisperFrom/" + Receiver + "/" + Message + "\n");
						}
					}
				}
			}

		}

		/**
		 * broadcast를 하는 함수입니다 저장되어있는 유저정보에 있는 모든 DataOutputStream에게 문자열을 전송합니다.
		 */
		private void broadcast(String string) throws IOException {
			for (UserInfo user : users) {
				user.getStream().writeUTF(string);
			}
		}
	}

	/**
	 * 유저 정보를 받는 클래스입니다. DataOutputStream정보와 이름이 저장되어있으며, getName, getStream함수를 통해
	 * 리턴할 수 있습니다.
	 * 
	 */
	private static class UserInfo {
		private DataOutputStream out;
		private String name;

		UserInfo(DataOutputStream o, String st) {
			out = o;
			name = st;
		}

		UserInfo(DataOutputStream o) {
			out = o;
		}

		UserInfo(String st) {
			name = st;
		}

		public DataOutputStream getStream() {
			return out;
		}

		public String getName() {
			return name;
		}
	}

}