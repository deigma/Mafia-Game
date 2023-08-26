import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServerTh extends Thread {

    private static final int MIN_PERSON = 5;
    private static DayTimer dayTimer = null;

    private ChatRoom gameRoom;
    private ChatRoom deadRoom;

    private Socket socket;
    private String userName;
    private BufferedReader reader;
    private PrintWriter writer;

    private Role role;
    private boolean dead;

    public ChatServerTh(Socket socket, ChatRoom gameRoom) {
        this.socket = socket;
        this.gameRoom = gameRoom;

        this.deadRoom = new ChatRoom();

        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUserName() {
        return userName;
    }

    public void writeln(String message) {
        writer.println(message);
        writer.flush();
    }

    public void write(String message) {
        writer.print(message);
        writer.flush();
    }

    @Override
    public void run() {
        try {
            userName = reader.readLine();
            System.out.println("name = " + userName);

            writeln("===직업 선택===");
            writeln("0. 마피아 1. 시민 2. 의사 3. 경찰");

            int select = -1;
            do {
                try {
                    String s = reader.readLine();
                    select = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    continue;
                }
            } while (!(0 <= select && select < Role.values().length));

            this.role = Role.getByRoleNum(select);

            writeln("당신은 " + role.getRoleName() + "입니다.");
            if (role.getRoleName().equals("Mafia")) {
                writeln("'/kill 이름' 명령어로 밤에 한명을 죽일 수 있습니다.");
                writeln("마피아와 시민이 같은 수가 되면 승리합니다.");
            }

            gameRoom.sendMessageAll(userName + "님이 입장하셨습니다.");

            while (gameRoom.getListSize() < MIN_PERSON) {
                String str = reader.readLine();

                gameRoom.sendMessageExceptMe(str, this.userName);
            }

            if (dayTimer == null) {
                dayTimer = new DayTimer();

                dayTimer.start();
            }

            while (true) {
                while (dayTimer.isDay()) {
                    doCitizen(dayTimer.isDay());
                }

                while (!dayTimer.isDay()) {
                    if (role == Role.Mafia) {
                        doMafia(dayTimer.isDay());
                    } else if (role == Role.Police) {
                        doPolice(dayTimer.isDay());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doMafia(boolean isDay) {
        if (!isDay) {
            try {
                String str = reader.readLine();

                Pattern pattern = Pattern.compile("/kill (\\w+)");
                Matcher matcher = pattern.matcher(str);
                if (matcher.matches()) {
                    gameRoom.sendMessageAll("악랄한 마피아가 " + matcher.group(1) + "님을 죽였습니다.");
                } else {
                    gameRoom.sendMessageAll(userName + ": " + str);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            doCitizen(isDay);
        }
    }

    private void doCitizen(boolean isDay) {
        if (isDay) {
            try {
                String str = reader.readLine();

                Pattern pattern = Pattern.compile("/vote (\\w+)");
                Matcher matcher = pattern.matcher(str);
                if (matcher.matches()) {
                    // TODO 투표 기능
                } else {
                    gameRoom.sendMessageAll(userName + ": " + str);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doPolice(boolean isDay) {
        if (!isDay) {
            try {
                String str = reader.readLine();

                Pattern pattern = Pattern.compile("/scan (\\w+)");
                Matcher matcher = pattern.matcher(str);

                if (matcher.matches()) {
                    // TODO 스캔 기능
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            doCitizen(isDay);
        }
    }

}
