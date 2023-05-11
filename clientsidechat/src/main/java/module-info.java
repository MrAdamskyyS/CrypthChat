module sample.clientsidechat {
    requires javafx.controls;
    requires javafx.fxml;


    opens sample.clientsidechat to javafx.fxml;
    exports sample.clientsidechat;
}