import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;

public class TaskManagerFX extends Application {
    private ArrayList<Task> tasks;
    private VBox taskPanel;
    private TextField taskInputField;
    private ComboBox<String> priorityComboBox;
    private CheckBox addDateTimeCheckBox;
    private DatePicker datePicker;
    private Spinner<Integer> hourSpinner;
    private Spinner<Integer> minuteSpinner;
    private static final String FILE_PATH = "tasks.txt"; // Changement de l'extension pour un fichier texte

    public TaskManagerFX() {
        tasks = new ArrayList<>();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gestionnaire de Tâches");
        primaryStage.setWidth(600);
        primaryStage.setHeight(600);

        // Créer une image pour le logo
        Image logo = new Image("logo.png");
        ImageView logoView = new ImageView(logo);
        logoView.setFitWidth(100);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);
        logoView.setCache(true);

        // Panneau principal d'affichage des tâches
        taskPanel = new VBox(10);
        taskPanel.setPadding(new Insets(10));
        taskPanel.setId("taskPanel");

        // ScrollPane pour contenir le panneau des tâches
        ScrollPane scrollPane = new ScrollPane(taskPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setPrefHeight(500);

        // Panneau de saisie et d'options
        HBox inputPanel = new HBox(10);
        inputPanel.setPadding(new Insets(10));
        inputPanel.setId("inputPanel");
        inputPanel.setAlignment(Pos.CENTER);

        // Champ de saisie pour ajouter des tâches
        taskInputField = new TextField();
        taskInputField.setPromptText("Entrez une tâche");

        // Liste déroulante pour les priorités
        String[] priorities = {"Faible", "Normale", "Haute"};
        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(priorities);
        priorityComboBox.setPromptText("Priorité");

        // Checkbox pour ajouter une date et une heure
        addDateTimeCheckBox = new CheckBox("Ajouter Date et Heure");

        // DatePicker pour la date
        datePicker = new DatePicker();
        datePicker.setDisable(true);
        datePicker.setPromptText("Sélectionnez une date");

        // Spinners pour l'heure et les minutes
        hourSpinner = new Spinner<>(0, 23, LocalTime.now().getHour());
        hourSpinner.setDisable(true);
        minuteSpinner = new Spinner<>(0, 59, LocalTime.now().getMinute());
        minuteSpinner.setDisable(true);

        addDateTimeCheckBox.setOnAction(e -> {
            boolean isSelected = addDateTimeCheckBox.isSelected();
            datePicker.setDisable(!isSelected);
            hourSpinner.setDisable(!isSelected);
            minuteSpinner.setDisable(!isSelected);
        });

        // Bouton pour ajouter une tâche
        Button addTaskButton = new Button("Ajouter tâche");
        addTaskButton.setOnAction(e -> addTask());

        inputPanel.getChildren().addAll(taskInputField, priorityComboBox, addDateTimeCheckBox, datePicker, hourSpinner, minuteSpinner, addTaskButton);

        // Panneau racine avec BorderPane
        BorderPane root = new BorderPane();
        HBox topPanel = new HBox(10);
        topPanel.setPadding(new Insets(10));
        StackPane centerPane = new StackPane(inputPanel);
        centerPane.setAlignment(Pos.CENTER);

        topPanel.getChildren().addAll(logoView, centerPane);
        topPanel.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(centerPane, Priority.ALWAYS);

                // Ajout de votre nom en bas à droite
        Label nameLabel = new Label("Chakhmoun Ilyass");
        nameLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold; -fx-cursor: hand;");
        nameLabel.setOnMouseClicked(event -> {
            openWebpage("https://github.com/Ilyasscytech");
        });

        root.setTop(topPanel);
        root.setCenter(scrollPane);

        // Charger les tâches depuis le fichier à l'ouverture
        loadTasksFromFile();

        // Créer la scène et l'ajouter à la scène principale
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

        // Méthode pour ouvrir une page web
    private void openWebpage(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour ajouter une tâche à la liste
    private void addTask() {
        String newTaskText = taskInputField.getText();
        String priority = priorityComboBox.getValue();
        Timestamp taskDate = null;

        if (!newTaskText.isEmpty() && priority != null) {
            if (addDateTimeCheckBox.isSelected()) {
                try {
                    LocalDate date = datePicker.getValue();
                    LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());

                    // Vérifier si la date et l'heure sont dans le passé
                    if (date.isBefore(LocalDate.now()) || (date.isEqual(LocalDate.now()) && time.isBefore(LocalTime.now()))) {
                        showAlert("Vous ne pouvez pas entrer une date ou une heure dans le passé.", "Erreur");
                        return;
                    }

                    taskDate = Timestamp.valueOf(date.atTime(time));
                } catch (Exception e) {
                    showAlert("Date ou heure invalide.", "Erreur");
                    return;
                }
            }

            Task newTask = new Task(newTaskText, priority, false, taskDate);
            tasks.add(newTask);
            taskInputField.clear();
            datePicker.setValue(null);
            hourSpinner.getValueFactory().setValue(LocalTime.now().getHour());
            minuteSpinner.getValueFactory().setValue(LocalTime.now().getMinute());
            updateTaskDisplay();
            saveTasksToFile(); // Sauvegarder les tâches après ajout
        } else {
            showAlert("Veuillez entrer une tâche et sélectionner une priorité.", "Erreur");
        }
    }

    // Méthode pour afficher les tâches
    private void updateTaskDisplay() {
        taskPanel.getChildren().clear();

        ArrayList<Task> completedTasks = new ArrayList<>();
        ArrayList<Task> incompleteTasks = new ArrayList<>();

        // Séparer les tâches terminées et non terminées
        for (Task task : tasks) {
            if (task.isDone()) {
                completedTasks.add(task);
            } else {
                incompleteTasks.add(task);
            }
        }

        // Trier les tâches sans date
        ArrayList<Task> tasksWithoutDate = new ArrayList<>();
        ArrayList<Task> tasksWithDate = new ArrayList<>();

        for (Task task : incompleteTasks) {
            if (task.getDate() == null) {
                tasksWithoutDate.add(task);
            } else {
                tasksWithDate.add(task);
            }
        }

        // Trier les tâches avec date par date
        tasksWithDate.sort(Comparator.comparing(Task::getDate));

        // Afficher les tâches sans date
        displayTasks(tasksWithoutDate, false);
        // Afficher les tâches avec date
        displayTasks(tasksWithDate, false);
        // Afficher les tâches terminées
        displayTasks(completedTasks, true);
    }

 private void displayTasks(ArrayList<Task> tasksToDisplay, boolean isDone) {
    for (Task task : tasksToDisplay) {
        HBox taskRow = new HBox(10);
        taskRow.setPadding(new Insets(5));
        taskRow.setPrefHeight(50);
        taskRow.getStyleClass().add("task-row");

        // Changer la couleur en fonction de la priorité ou si la tâche est terminée
        if (isDone) {
            taskRow.setStyle("-fx-background-color: lightgray;"); // Grise si validée
        } else {
            switch (task.getPriority()) {
                case "Faible":
                    taskRow.setStyle("-fx-background-color: lightgreen;");
                    break;
                case "Normale":
                    taskRow.setStyle("-fx-background-color: lightyellow;");
                    break;
                case "Haute":
                    taskRow.setStyle("-fx-background-color: lightcoral;");
                    break;
            }
        }

        // Créer le label pour la tâche
        Label taskLabel = new Label(task.getText());

        // Créer un HBox pour les boutons à droite
        HBox buttonPanel = new HBox(5);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);

        // Bouton pour supprimer la tâche
        Button removeButton = new Button("Supprimer");
        removeButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        removeButton.setOnAction(e -> {
            removeTask(task);
        });

        // Ajouter le bouton de validation et la description de la tâche
        if (isDone) {
            Label doneLabel = new Label("Tâche validée");
            doneLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            taskRow.getChildren().addAll(doneLabel, taskLabel, buttonPanel);
        } else {
            // Ajouter le bouton de validation uniquement pour les tâches non terminées
            Button validateButton = new Button("Terminer");
            validateButton.setStyle("-fx-background-color: lightblue;");
            validateButton.setOnAction(e -> {
                markTaskAsDone(task);
            });
            taskRow.getChildren().addAll(validateButton, taskLabel, buttonPanel);
        }

        // Ajouter le bouton de suppression au panneau des boutons
        buttonPanel.getChildren().add(removeButton);

        // Étendre le panneau des boutons pour qu'il soit centré à droite
        HBox.setHgrow(taskLabel, Priority.ALWAYS);
        HBox.setHgrow(buttonPanel, Priority.ALWAYS);

        taskPanel.getChildren().add(taskRow);
    }
}

    // Méthode pour marquer une tâche comme terminée
    private void markTaskAsDone(Task task) {
        task.setDone(true); // Marquer la tâche comme terminée
        updateTaskDisplay(); // Rafraîchir l'affichage des tâches
        saveTasksToFile(); // Sauvegarder les tâches après mise à jour
    }

    // Méthode pour supprimer une tâche
    private void removeTask(Task task) {
        tasks.remove(task);
        updateTaskDisplay();
        saveTasksToFile(); // Sauvegarder les tâches après suppression
    }

    // Méthode pour afficher une alerte
    private void showAlert(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Méthode pour charger les tâches à partir du fichier
    private void loadTasksFromFile() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    String text = parts[0];
                    String priority = parts[1];
                    boolean isDone = Boolean.parseBoolean(parts[2]);
                    Timestamp date = parts.length > 3 ? Timestamp.valueOf(parts[3]) : null;

                    Task task = new Task(text, priority, isDone, date);
                    tasks.add(task);
                }
                updateTaskDisplay(); // Rafraîchir l'affichage après chargement
            } catch (IOException e) {
                showAlert("Erreur de chargement des tâches : " + e.getMessage(), "Erreur");
            }
        }
    }

    // Méthode pour sauvegarder les tâches dans un fichier
    private void saveTasksToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Task task : tasks) {
                String line = task.getText() + ";" + task.getPriority() + ";" + task.isDone() + ";" +
                        (task.getDate() != null ? task.getDate().toString() : "");
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            showAlert("Erreur de sauvegarde des tâches : " + e.getMessage(), "Erreur");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
