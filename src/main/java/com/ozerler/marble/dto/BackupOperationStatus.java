package com.ozerler.marble.dto;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

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
        String value;
        if ("BACKUP".equals(operationType)) {
            value = "Yedek alma";
        } else {
            value = "Geri yükleme";
        }
        this.message = value + " işlemi başlatıldı...";
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
