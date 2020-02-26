package MusicPlayer;

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
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;

public class Controller {


    @FXML
    private Label titleOfSong;
    @FXML
    private Slider songSliderBar, volumeSlideBar;
    @FXML
    private Button previous, playPause, next, shuffle;
    @FXML
    private Text currentSongDuration, totalSongDuration_remainingTime;

    private MediaPlayer mediaPlayer;
    private File songFile;
    private File[] songQueue;
    private int currentSongIndex = 0;
    private boolean isSongDurationToggled = false;
    private String[] allowedFiles = new String[]{"mp3", "mp4", "aif", "aiff", "wav", "fxm", "flv"};

    //No argument constructor used to set controller instance in FXML
    public Controller() {
    }


    @FXML
    protected void chooseSongWasClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select music file");
        //Get scenegraph by referencing node already added
        songFile = fileChooser.showOpenDialog(titleOfSong.getScene().getWindow());
        if (songFile == null) {
            throw new NullPointerException("Song Chooser cannot be null");
        } else {
            checkFileType(songFile);
            createMediaPlayerWithFile();
            createMediaElements();
        }
    }

    @FXML
    protected void chooseDirectoryWasClicked() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select folder containing music");
        //Get scenegraph by referencing node already added
        File songPath = directoryChooser.showDialog(titleOfSong.getScene().getWindow());
        songQueue = songPath.listFiles();
        if (songQueue == null) {
            throw new NullPointerException("Directory chooser cannot be null");
        } else {
            checkFileType(songFile);
            createMediaPlayerWithDirectory();
            createMediaElements();
            next.setDisable(false);
            previous.setDisable(false);
            shuffle.setDisable(false);
        }
    }

    public void initialize() {
        addAllListeners();
        setElementsInitialState();
    }

    private void setElementsInitialState() {
        playPause.setDisable(true);
        songSliderBar.setDisable(true);
        next.setDisable(true);
        previous.setDisable(true);
        shuffle.setDisable(true);
        volumeSlideBar.setDisable(true);
    }

    private void checkFileType(File file) {
        String fileType = null;

        try {
            //If a single song was selected
            if (songFile != null)
                fileType = Files.probeContentType(songFile.toPath());
            //If a directory was selected
            if (songQueue != null) {
                for (File file1 : songQueue)
                    fileType = Files.probeContentType(file1.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert fileType != null;
        fileType = fileType.substring(fileType.length() - 3);

        for (String fileTypeExtension : allowedFiles) {
            if (fileTypeExtension.equals(fileType)) {
                return;
            }
        }
        throw new UnsupportedOperationException("Selected File is incompatible!");
    }

    private void createMediaElements() {

        //Prevent overlap from selecting another song while one is already playing
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING || mediaPlayer.getStatus() == MediaPlayer.Status.PAUSED)
            mediaPlayer.stop();

        mediaPlayer.setAutoPlay(true);
        playPause.setDisable(false);
        playPause.setGraphic(new ImageView(new Image("assets/pause.png")));

        handleMediaPlayerWhenReady();
        syncSongSlideBar();
        handleEndOfSongInQueue();
    }

    private void addAllListeners() {
        addListenerToSongSlideBar();
        addListenerToVolumeSlideBar();
        addListenerToPlayPauseButton();
    }

    private void addListenerToVolumeSlideBar() {

        volumeSlideBar.valueProperty().addListener(observable -> {
            if (volumeSlideBar.isValueChanging())
                mediaPlayer.setVolume(volumeSlideBar.getValue() / 100);
        });

        volumeSlideBar.setOnMouseClicked(mouseEvent -> {
            volumeSlideBar.setValueChanging(true);
            double value = (mouseEvent.getX() / volumeSlideBar.getWidth()) * volumeSlideBar.getMax();
            mediaPlayer.setVolume(value / 100.0);
            volumeSlideBar.setValueChanging(false);
        });
    }

    //Listener for asynchronous MediaPlayer object
    private void handleMediaPlayerWhenReady() {
        mediaPlayer.setOnReady(() -> {
            songSliderBar.setDisable(false);
            volumeSlideBar.setDisable(false);
            volumeSlideBar.setValue(1.0);
            songSliderBar.setMax(mediaPlayer.getTotalDuration().toSeconds());
            //Sets the title and removed the file type
            String songPlaying = new File(mediaPlayer.getMedia().getSource()).getName().replace("%20", " ");
            titleOfSong.setText(songPlaying.substring(0, songPlaying.length() - 4));

            //Listener for time passed which sets currentTime
            mediaPlayer.currentTimeProperty().addListener((observableValue, oldTime, newTime) -> {

                //Get current time and total time to calculate remaining time
                Double currentTime = newTime.toSeconds();
                Double totalTime = mediaPlayer.getTotalDuration().toSeconds();
                int remainingTime = (int) (totalTime - currentTime);

                currentSongDuration.setText(formatSeconds(newTime));

                //If songDuration element is toggled then switch between remaining time or totalTime
                if (isSongDurationToggled) {
                    totalSongDuration_remainingTime.setText(formatSeconds(mediaPlayer.getTotalDuration()));
                } else {
                    totalSongDuration_remainingTime.setText(formatSeconds(remainingTime));
                }

            });

            totalSongDuration_remainingTime.setOnMouseClicked(mouseEvent -> isSongDurationToggled = !isSongDurationToggled);
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
            return String.format("%02d", minutes) + ":" + String.format("%02d", seconds);
        } catch (IllegalArgumentException e) {
            return "Object passed to formatSeconds needs to be of type Duration or Integer but was of type : " + object.getClass();
        }
    }

    private void addListenerToSongSlideBar() {

        songSliderBar.valueProperty().addListener(observable -> {
            if (songSliderBar.isValueChanging())
                mediaPlayer.seek(Duration.seconds(songSliderBar.getValue()));
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

    private void createMediaPlayerWithFile() {
        Media media = new Media(songFile.toURI().toString());
        mediaPlayer = new MediaPlayer(media);
    }

    private void createMediaPlayerWithDirectory() {
        Media media = new Media(songQueue[currentSongIndex].toURI().toString());
        mediaPlayer = new MediaPlayer(media);
    }

    @FXML
    private void shuffleSong() {
        int newIndex = new Random().nextInt(songQueue.length);
        if (currentSongIndex != newIndex) {
            currentSongIndex = newIndex;
            createMediaPlayerWithDirectory();
            createMediaElements();
        } else { //Handle shuffling to the same song
            shuffleSong();
            System.out.println("Called shuffle again");
        }
    }

    //If there is a queue then go to next song in queue once current song is finished
    private void handleEndOfSongInQueue() {
        mediaPlayer.currentTimeProperty().addListener(observable -> {
            if (songQueue != null && currentSongIndex != songQueue.length) {
                //cast to int to ignore decimal places
                int currentTime = (int) mediaPlayer.getCurrentTime().toSeconds();
                if (mediaPlayer.getStopTime().toSeconds() == currentTime)
                    nextSong();
            }
        });
    }

    //Plays next song by increasing index
    private void nextSong() {
        currentSongIndex++;
        createMediaPlayerWithDirectory();
        createMediaElements();
    }

    @FXML
    private void onPreviousClick() {
        if (songQueue != null && currentSongIndex > 0) {
            currentSongIndex--;
            System.out.println(currentSongIndex);
        }
        createMediaPlayerWithDirectory();
        createMediaElements();
    }

    @FXML
    private void onNextClick() {
        if (songQueue != null && currentSongIndex < songQueue.length - 1) {
            nextSong();
        }
    }
}
