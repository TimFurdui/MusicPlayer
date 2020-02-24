package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private Button previous, playPause, next;
    @FXML
    private Text currentSongDuration, totalSongDuration_remainingTime;

    private MediaPlayer mediaPlayer;
    private File songFile;
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
        File songPath = directoryChooser.showDialog(titleOfSong.getScene().getWindow());
        songQueue = songPath.listFiles();
        createMediaElements();
    }

    public void initialize() {
        addAllListeners();
        playPause.setDisable(true);
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
        playPause.setDisable(false);
        playPause.setGraphic(new ImageView(new Image("assets/pause.png")));

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
        mediaPlayer.setOnReady(() -> {
            songSliderBar.setMax(mediaPlayer.getTotalDuration().toSeconds());
            //Sets the title and removed the file type
            String songPlaying = new File(mediaPlayer.getMedia().getSource()).getName().replace("%20", " ");
            titleOfSong.setText(songPlaying.substring(0, songPlaying.length() - 4));

            //Listener for time passed which sets currentTime
            mediaPlayer.currentTimeProperty().addListener((observableValue, oldTime, newTime) -> {
                Double currentTime = newTime.toSeconds();
                Double totalTime = mediaPlayer.getTotalDuration().toSeconds();
                int remainingTime = (int) (totalTime - currentTime);

                currentSongDuration.setText(formatSeconds(newTime));
                totalSongDuration_remainingTime.setText(formatSeconds(remainingTime));
            });
            //TODO on click of this element toggle between showing totalTime/remainingTime
//                totalSongDuration_remainingTime.setText(String.valueOf((int) mediaPlayer.getTotalDuration().toSeconds()));
        });
    }

    //TODO when integer is in single digits append 0 in front to keep 00:00 format
    //Formats seconds to minutes and seconds
    private String formatSeconds(Object object) {
        int seconds = 0;
        try {

            if (object instanceof Duration) {
                seconds = (int) ((Duration) object).toSeconds();
            }
            if (object instanceof Integer) {
                seconds = (Integer) object;
            }

            int minutes = seconds / 60;
            seconds %= 60;
            return minutes + ":" + seconds;

        } catch (IllegalArgumentException e) {
            return "Object passed to formatSeconds needs to be of type Duration or Integer but was of type : " + object.getClass();
        }
    }

    private void addListenerToSlideBarElements() {
        songSliderBar.valueProperty().addListener(observable -> {
            if (songSliderBar.isValueChanging()) {
                mediaPlayer.seek(Duration.seconds(songSliderBar.getValue()));
            }
        });
        songSliderBar.setOnMouseClicked(mouseEvent -> {
            songSliderBar.setValueChanging(true);
            double value = (mouseEvent.getX() / songSliderBar.getWidth()) * songSliderBar.getMax();
            songSliderBar.setValue(value);
            songSliderBar.setValueChanging(false);
        });

    }

    private void addListenerToPlayPauseButton() {
        playPause.setOnAction(actionEvent -> {
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playPause.setGraphic(new ImageView(new Image("assets/play.png")));
            }
            if (mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED) {
                mediaPlayer.play();
                playPause.setGraphic(new ImageView(new Image("assets/pause.png")));
            }
        });
    }

    //Syncs the songs SlideBar with the current duration of the song
    private void syncSongSlideBar() {
        mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> songSliderBar.setValue(newValue.toSeconds()));
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
        mediaPlayer.currentTimeProperty().addListener(observable -> {
            if (songQueue != null && currentSongIndex != songQueue.length) {

                //cast to int to ignore decimal places
                int currentTime = (int) mediaPlayer.getCurrentTime().toSeconds();
                int remainingTime = (int) mediaPlayer.getTotalDuration().toSeconds();
                if (currentTime == remainingTime)
                    nextSong();
            }
        });
    }

    private void nextSong() {
        currentSongIndex++;
        createMediaElements();
    }

    @FXML
    private void onPreviousClick() {
        if (songQueue != null && currentSongIndex > 0) {
            currentSongIndex--;
            System.out.println(currentSongIndex);
        }
        createMediaElements();
    }

    @FXML
    private void onNextClick() {
        if (songQueue != null && currentSongIndex < songQueue.length - 1) {

            nextSong();
            System.out.println(currentSongIndex);
        }
    }
    //Move name to helper function
    //Move slider to helper function

}
