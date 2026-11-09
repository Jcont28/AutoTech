package hn.autotech.ui;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Tareas {
    private Tareas() {
    }

    public static <T> void ejecutar(Component padre, Supplier<T> trabajo, Consumer<T> alTerminar) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return trabajo.get();
            }

            @Override
            protected void done() {
                try {
                    alTerminar.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    Mensajes.error(padre, e.getCause());
                }
            }
        }.execute();
    }

    public static <T> void ejecutar(Component padre, Supplier<T> trabajo, Consumer<T> alTerminar, Consumer<Throwable> alFallar) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return trabajo.get();
            }

            @Override
            protected void done() {
                try {
                    alTerminar.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException e) {
                    alFallar.accept(e.getCause());
                }
            }
        }.execute();
    }

    public static void ejecutar(Component padre, Runnable trabajo, Runnable alTerminar) {
        ejecutar(padre, () -> {
            trabajo.run();
            return Boolean.TRUE;
        }, resultado -> alTerminar.run());
    }
}
