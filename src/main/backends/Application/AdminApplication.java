package Application;

import controllers.UserSession;
import controllers.ViewLoader;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import Launcher.Launcher;

public class AdminApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        UserSession.initConnection(Launcher.serverIp, 9999);
        Scene scene = new Scene(ViewLoader.load("AdminInfo.fxml"));
        stage.setTitle("Thong tin admin");
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        System.out.println("Đang đóng chương trình Admin...");
        System.exit(0);
    }
}
