package sample;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;

public class Controller {


    @FXML
    private Label titleOfSong;
    @FXML
    private Slider songSliderBar;
    @FXML
    private Button fileChooserButton, next, previous, playPause, directoryChooserButton;
    @FXML
    private ButtonBar fileButtonBar, musicButtonBar;
    @FXML
    private HBox sliderElement;
    @FXML
    private Text currentSongDuration, totalSongDuration_remainingTime;

    private MediaPlayer mediaPlayer;
    private File songFile, songPath;
    private File[] songQueue;
    private int currentSongIndex = 0;

    //No argument constructor used to set controller instance in FXML
    public Controller() {
    }


    @FXML
    protected void chooseSongWasClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select music file");
        //Get scenegraph by referencing node already added

        songFile = fileChooser.showOpenDialog(titleOfSong.getScene().getWindow());
        createMediaElements();
    }

    @FXML
    protected void chooseDirectoryWasClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder containing music");
        //Get scenegraph by referencing node already added
        songPath = directoryChooser.showDialog(titleOfSong.getScene().getWindow());
        songQueue = songPath.listFiles();
        createMediaElements();
    }

    public void initialize() {
        addAllListeners();
        playPause.setGraphic(new ImageView(new Image("assets/play.png")));
    }

    private void createMediaElements() {
        if (songFile != null)
            handleSingleSong();
        if (songQueue != null)
            handleSongDirectory();

        //Prevent overlap from selecting another song while one is already playing
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING)
            mediaPlayer.stop();

        mediaPlayer.setAutoPlay(true);

        //Since the MediaPlayer is asynchronous we need to set a listener for when it's ready
        handleMediaPlayerWhenReady();
        syncSongSlideBar();
        handleEndOfSongInQueue();
    }

    private void addAllListeners() {
        addListenerToSlideBarElements();
        addListenerToPlayPauseButton();
    }

    //Listener for asynchronous MediaPlayer object
    private void handleMediaPlayerWhenReady() {
        mediaPlayer.setOnReady(new Runnable() {
            @Override
            public void run() {
                songSliderBar.setMax(mediaPlayer.getTotalDuration().toSeconds());
                //Sets the title and removed the file type
                String songPlaying = new File(mediaPlayer.getMedia().getSource()).getName().replace("%20", " ");
                titleOfSong.setText(songPlaying.substring(0, songPlaying.length() - 4));

                //Listener for time passed which sets currentTime
                mediaPlayer.currentTimeProperty().addListener((observableValue, oldTime, newTime) -> {
                    int currentTime = (int) newTime.toSeconds();
                    int totalTime = (int) mediaPlayer.getTotalDuration().toSeconds();
                    int remainingTime = totalTime - currentTime;

                    currentSongDuration.setText(String.valueOf(currentTime));
                    totalSongDuration_remainingTime.setText(String.valueOf(remainingTime));
                });
                //TODO on click of this element toggle between showing totalTime/remainingTime
//                totalSongDuration_remainingTime.setText(String.valueOf((int) mediaPlayer.getTotalDuration().toSeconds()));
            }
        });
    }

    private void addListenerToSlideBarElements() {
        songSliderBar.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (songSliderBar.isValueChanging()) {
                    mediaPlayer.seek(Duration.seconds(songSliderBar.getValue()));
                }
            }
        });
        songSliderBar.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                songSliderBar.setValueChanging(true);
                double value = (mouseEvent.getX() / songSliderBar.getWidth()) * songSliderBar.getMax();
                songSliderBar.setValue(value);
                songSliderBar.setValueChanging(false);
            }
        });

    }

    private void addListenerToPlayPauseButton() {
        playPause.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                    playPause.setGraphic(new ImageView(new Image("assets/play.png")));
                } else {
                    mediaPlayer.play();
                    playPause.setGraphic(new ImageView(new Image("assets/pause.png")));
                }
            }
        });
    }

    //Syncs the songs SlideBar with the current duration of the song
    private void syncSongSlideBar() {
        mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> {
            songSliderBar.setValue(newValue.toSeconds());
        });
    }

    private void handleSingleSong() {
        Media media = new Media(songFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
    }

    private void handleSongDirectory() {
        Media media = new Media(songQueue[currentSongIndex].toURI().toString());
        mediaPlayer = new MediaPlayer(media);
    }

    //If there is a queue then go to next song in queue once current song is finished
    private void handleEndOfSongInQueue() {
        mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (songQueue != null && currentSongIndex != songQueue.length) {
                    //cast to int to ignore decimal places
                    int currentTime = (int) mediaPlayer.getCurrentTime().toSeconds();
                    int remainingTime = (int) mediaPlayer.getTotalDuration().toSeconds();
                    if (currentTime == remainingTime)
                        nextSong();
                }
            }
        });
    }

    private void nextSong() {
        currentSongIndex++;
        createMediaElements();
    }

    //Move name to helper function
    //Move slider to helper function

}
