package dev.xef2.visualkeymap.gui;

import net.minecraft.client.Minecraft;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.lwjgl.sdl.SDLDialog.*;
import static org.lwjgl.sdl.SDLProperties.*;

/**
 * Owns the native resources required by SDL's asynchronous file dialogs.
 */
public final class NativeFileDialog {
    private static final Set<Request> ACTIVE_REQUESTS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentLinkedQueue<Result> RESULTS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Request> CLEANUP = new ConcurrentLinkedQueue<>();

    private NativeFileDialog() {
    }

    public static void showOpen(String title, Consumer<Path> selectedPathConsumer) {
        show(SDL_FILEDIALOG_OPENFILE, title, null, selectedPathConsumer);
    }

    public static void showSave(String title, Path defaultPath, Consumer<Path> selectedPathConsumer) {
        show(SDL_FILEDIALOG_SAVEFILE, title, defaultPath, selectedPathConsumer);
    }

    private static void show(int type, String title, Path defaultPath, Consumer<Path> selectedPathConsumer) {
        Request request = new Request(type, title, defaultPath, selectedPathConsumer);
        ACTIVE_REQUESTS.add(request);
        if (!request.show()) {
            ACTIVE_REQUESTS.remove(request);
            request.close();
        }
    }

    /**
     * Runs completed dialog actions on the client thread. Native callback resources are released on
     * the following tick so they cannot be freed while SDL is still returning from the callback.
     */
    public static void poll() {
        Request request;
        while ((request = CLEANUP.poll()) != null) {
            ACTIVE_REQUESTS.remove(request);
            request.close();
        }

        Result result;
        while ((result = RESULTS.poll()) != null) {
            try {
                if (result.path() != null) {
                    result.request().selectedPathConsumer.accept(result.path());
                }
            } finally {
                CLEANUP.add(result.request());
            }
        }
    }

    private record Result(Request request, Path path) {
    }

    private static final class Request implements AutoCloseable {
        private final int type;
        private final String title;
        private final Path defaultPath;
        private final Consumer<Path> selectedPathConsumer;
        private final SDL_DialogFileFilter.Buffer filters;
        private final ByteBuffer filterName;
        private final ByteBuffer filterPattern;
        private final SDL_DialogFileCallback callback;
        private final AtomicBoolean completed = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        private int properties;

        private Request(int type, String title, Path defaultPath, Consumer<Path> selectedPathConsumer) {
            this.type = type;
            this.title = title;
            this.defaultPath = defaultPath;
            this.selectedPathConsumer = selectedPathConsumer;
            this.filterName = MemoryUtil.memUTF8("JSON");
            this.filterPattern = MemoryUtil.memUTF8("json");
            this.filters = SDL_DialogFileFilter.calloc(1);
            this.filters.get(0).set(this.filterName, this.filterPattern);
            this.callback = SDL_DialogFileCallback.create(this::complete);
        }

        private boolean show() {
            this.properties = SDL_CreateProperties();
            if (this.properties == 0) {
                return false;
            }

            boolean configured = SDL_SetPointerProperty(
                    this.properties, SDL_PROP_FILE_DIALOG_FILTERS_POINTER, this.filters.address()
            );
            configured &= SDL_SetNumberProperty(
                    this.properties, SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER, this.filters.remaining()
            );
            configured &= SDL_SetPointerProperty(
                    this.properties,
                    SDL_PROP_FILE_DIALOG_WINDOW_POINTER,
                    Minecraft.getInstance().getWindow().handle()
            );
            configured &= SDL_SetBooleanProperty(this.properties, SDL_PROP_FILE_DIALOG_MANY_BOOLEAN, false);
            configured &= SDL_SetStringProperty(this.properties, SDL_PROP_FILE_DIALOG_TITLE_STRING, this.title);
            if (this.defaultPath != null) {
                configured &= SDL_SetStringProperty(
                        this.properties,
                        SDL_PROP_FILE_DIALOG_LOCATION_STRING,
                        this.defaultPath.toAbsolutePath().toString()
                );
            }
            if (!configured) {
                return false;
            }

            SDL_ShowFileDialogWithProperties(this.type, this.callback, MemoryUtil.NULL, this.properties);
            return true;
        }

        private void complete(long userdata, long filelist, int filter) {
            if (!this.completed.compareAndSet(false, true)) {
                return;
            }

            Path selectedPath = null;
            if (filelist != MemoryUtil.NULL) {
                long firstPath = MemoryUtil.memGetAddress(filelist);
                if (firstPath != MemoryUtil.NULL) {
                    selectedPath = Path.of(MemoryUtil.memUTF8(firstPath));
                }
            }
            RESULTS.add(new Result(this, selectedPath));
        }

        @Override
        public void close() {
            if (!this.closed.compareAndSet(false, true)) {
                return;
            }
            if (this.properties != 0) {
                SDL_DestroyProperties(this.properties);
                this.properties = 0;
            }
            this.callback.free();
            this.filters.free();
            MemoryUtil.memFree(this.filterPattern);
            MemoryUtil.memFree(this.filterName);
        }
    }
}
