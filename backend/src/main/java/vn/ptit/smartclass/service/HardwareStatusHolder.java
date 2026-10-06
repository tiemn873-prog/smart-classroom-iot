package vn.ptit.smartclass.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/** Nho trang thai song/chet cua ESP32 (lay tu topic hardware/status, co che LWT). */
@Component
public class HardwareStatusHolder {

    private final AtomicBoolean online = new AtomicBoolean(false);

    public boolean isOnline() {
        return online.get();
    }

    public void setOnline(boolean value) {
        online.set(value);
    }
}
