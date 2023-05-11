module sample.serversidechat {
    requires javafx.controls;
    requires javafx.fxml;


    opens sample.serversidechat to javafx.fxml;
    exports sample.serversidechat;
}