package de.phlup.circuitchaos.client.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioSupplier {

    private volatile Clip audioClip;

    private final Object         sync = new Object();
    private final ResourceLoader resourceLoader;

    @Setter
    private String themePath = "classpath:themes/default";

    @Async
    public void playSound(String file) {
        Resource resource = resourceLoader.getResource(themePath + file);

        if (!resource.exists() || !resource.isReadable()) {
            log.warn("Couldn't play sound - could not find or read file {}", file);
            return;
        }

        try (AudioInputStream audioInputStream =
                     AudioSystem.getAudioInputStream(resource.getInputStream())) {

            synchronized (sync) {
                stop();

                Clip newClip = AudioSystem.getClip();

                newClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        synchronized (sync) {
                            if (audioClip == newClip) {
                                audioClip = null;
                                newClip.close();
                            }
                        }
                    }
                });

                newClip.open(audioInputStream);
                audioClip = newClip;

                log.debug("Playing sound {}", file);
                newClip.start();
            }

        } catch (Exception e) {
            log.warn("Couldn't play sound - {}: {}", e.getClass().getSimpleName(), e.getMessage());
            log.debug("Couldn't play sound - {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    public void stop() {
        synchronized (sync) {
            if (audioClip != null) {
                log.debug("Stopping playing sound");
                audioClip.stop();
                audioClip.close();
                audioClip = null;
            }
        }
    }
}
