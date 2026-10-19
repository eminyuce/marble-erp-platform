package com.ozerler.marble.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class BackupOperationStatus {

    public enum State {
        IDLE, RUNNING, SUCCESS, FAILED
    }

    private State state = State.IDLE;
    private String operationType; // "BACKUP" or "RESTORE"
    private String message;
    private String currentFile;
    private Instant startTime;
    private Instant finishTime;

    public synchronized void start(String operationType, String currentFile) {
        this.state = State.RUNNING;
        this.operationType = operationType;
        this.currentFile = currentFile;
        this.startTime = Instant.now();
        this.finishTime = null;
        this.message = ("BACKUP".equals(operationType) ? "Yedek alma" : "Geri yükleme") + " işlemi başlatıldı...";
    }

    public synchronized void success(String message) {
        this.state = State.SUCCESS;
        this.message = message;
        this.finishTime = Instant.now();
    }

    public synchronized void fail(String errorMessage) {
        this.state = State.FAILED;
        this.message = errorMessage;
        this.finishTime = Instant.now();
    }
}
