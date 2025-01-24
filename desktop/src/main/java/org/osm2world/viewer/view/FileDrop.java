package org.osm2world.viewer.view;

import static java.util.Arrays.stream;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.*;

/*
 * the initial version of this class has been copied from the Public Domain resource
 * http://www.iharder.net/current/java/filedrop/
 */

/**
 * This class makes it easy to drag and drop files from the operating
 * system to a Java program. Any <tt>java.awt.Component</tt> can be
 * dropped onto, but only <tt>javax.swing.JComponent</tt>s will indicate
 * the drop event with a changed border.
 * <p/>
 * To use this class, construct a new <tt>FileDrop</tt> by passing
 * it the target component and a <tt>Listener</tt> to receive notification
 * when file(s) have been dropped. Here is an example:
 * <p/>
 * <code><pre>
 *      JPanel myPanel = new JPanel();
 *      new FileDrop( myPanel, new FileDrop.Listener()
 *      {   public void filesDropped( java.io.File[] files )
 *          {
 *              // handle file drop
 *              ...
 *          }   // end filesDropped
 *      }); // end FileDrop.Listener
 * </pre></code>
 * <p/>
 * You can specify the border that will appear when files are being dragged by
 * calling the constructor with a <tt>javax.swing.border.Border</tt>. Only
 * <tt>JComponent</tt>s will show any indication with a border.
 * <p/>
 * You can turn on some debugging features by passing a <tt>PrintStream</tt>
 * object (such as <tt>System.out</tt>) into the full constructor. A <tt>null</tt>
 * value will result in no extra debugging information being output.
 * <p/>
 *
 * <p>I'm releasing this code into the Public Domain. Enjoy.
 * </p>
 * <p><em>Original author: Robert Harder, rharder@usa.net</em></p>
 * <p>2007-09-12 Nathan Blomquist -- Linux (KDE/Gnome) support added.</p>
 *
 * @author  Robert Harder
 * @author  rharder@users.sf.net
 * @version 1.0.1
 */
class FileDrop
{
    private transient javax.swing.border.Border normalBorder;
    private transient DropTargetListener dropListener;

    private static Boolean supportsDnD;

    // Default border color
    private static final java.awt.Color defaultBorderColor = new java.awt.Color( 0f, 0f, 1f, 0.25f );

    /**
     * Constructs a {@link FileDrop} with a default light-blue border
     * and, if <var>c</var> is a {@link Container}, recursively
     * sets all elements contained within as drop targets, though only
     * the top level container will change borders.
     *
     * @param c Component on which files will be dropped.
     * @param listener Listens for <tt>filesDropped</tt>.
     */
    public FileDrop(final Component c, final Consumer<File[]> listener) {
        this(
                c,
                javax.swing.BorderFactory.createMatteBorder(2, 2, 2, 2, defaultBorderColor), // Drag border
                true,
                listener);
    }

    /**
     * Full constructor with a specified border.
     *
     * @param c Component on which files will be dropped.
     * @param dragBorder Border to use on <tt>JComponent</tt> when dragging occurs.
     * @param recursive Recursively set children as drop targets.
     * @param listener Listens for <tt>filesDropped</tt>.
     */
    public FileDrop(
            final Component c,
            final javax.swing.border.Border dragBorder,
            final boolean recursive,
            final Consumer<File[]> listener) {

        if (supportsDnD()) {

            // Make a drop listener
            dropListener = new DropTargetListener() {

                public void dragEnter(DropTargetDragEvent evt) {

                    // Is this an acceptable drag event?
                    if (isDragOk(evt)) {
                        // If it's a Swing component, set its border
                        if (c instanceof JComponent jc) {
                            normalBorder = jc.getBorder();
                            jc.setBorder(dragBorder);
                        }
                        // Acknowledge that it's okay to enter
                        evt.acceptDrag(DnDConstants.ACTION_COPY);
                    } else {   // Reject the drag event
                        evt.rejectDrag();
                    }

                }

                public void dragOver(DropTargetDragEvent evt) {
                    // This is called continually as long as the mouse is over the drag target.
                }

                public void drop(DropTargetDropEvent evt) {
                    try {   // Get whatever was dropped
                        Transferable tr = evt.getTransferable();

                        // Is it a file list?
                        if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            // Say we'll take it.
                            evt.acceptDrop(DnDConstants.ACTION_COPY);

                            @SuppressWarnings("unchecked")
                            List<File> fileList = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                            final File[] files = fileList.toArray(new File[0]);

                            // Alert listener to drop.
                            if (listener != null) {
                                listener.accept(files);
                            }

                            // Mark that drop is completed.
                            evt.getDropTargetContext().dropComplete(true);
                        } else {
                            // this section will check for a reader flavor.

                            DataFlavor[] flavors = tr.getTransferDataFlavors();
                            boolean handled = false;
                            for (DataFlavor flavor : flavors) {
                                if (flavor.isRepresentationClassReader()) {
                                    // Say we'll take it.
                                    evt.acceptDrop(DnDConstants.ACTION_COPY);

                                    Reader reader = flavor.getReaderForText(tr);

                                    BufferedReader br = new BufferedReader(reader);

                                    if (listener != null) {
                                        listener.accept(createFileArray(br));
                                    }

                                    // Mark that drop is completed.
                                    evt.getDropTargetContext().dropComplete(true);
                                    handled = true;
                                    break;
                                }
                            }
                            if (!handled) {
                                System.err.println("FileDrop: not a file list or reader - abort.");
                                evt.rejectDrop();
                            }
                        }
                    } catch (IOException | UnsupportedFlavorException e) {
                        System.err.println("FileDrop: IOException - abort:");
                        e.printStackTrace();
                        evt.rejectDrop();
                    } finally {
                        // If it's a Swing component, reset its border
                        if (c instanceof JComponent jc) {
                            jc.setBorder(normalBorder);
                        }
                    }
                }

                public void dragExit(java.awt.dnd.DropTargetEvent evt) {
                    // If it's a Swing component, reset its border
                    if (c instanceof JComponent jc) {
                        jc.setBorder(normalBorder);
                    }
                }

                public void dropActionChanged(DropTargetDragEvent evt) {
                    if (isDragOk(evt)) {
                        evt.acceptDrag(DnDConstants.ACTION_COPY);
                    } else {
                        evt.rejectDrag();
                    }
                }

            };

            // Make the component (and possibly children) drop targets
            makeDropTarget(c, recursive);

        } else {
            System.err.println("FileDrop: Drag and drop is not supported with this JVM");
        }
    }

    /** Discover if the running JVM is modern enough to have drag and drop. */
    private static boolean supportsDnD() {
        if (supportsDnD == null) {
            try {
                // attempt to load an arbitrary DnD class
                Class.forName("java.awt.dnd.DnDConstants");
                supportsDnD = true;
            } catch (Exception e) {
                supportsDnD = false;
            }
        }
        return supportsDnD;
    }

    private static File[] createFileArray(BufferedReader bReader) {

        List<File> result = new ArrayList<>();

        bReader.lines()
                .filter(line -> !("" + (char) 0).equals(line)) // kde seems to append a 0 char to the end of the reader
                .forEach(line -> {
                    try {
                        result.add(new File(new URI(line)));
                    } catch (URISyntaxException e) {
                        System.err.println("Error with " + line + ": " + e.getMessage());
                    }
                });

        return result.toArray(new File[0]);

    }

    private void makeDropTarget(final Component c, boolean recursive) {

        // Listen for hierarchy changes and remove the drop target when the parent gets cleared out.
        c.addHierarchyListener(evt -> {
            if (c.getParent() == null) {
                c.setDropTarget(null);
            } else {
                new DropTarget(c, dropListener);
            }
        });

        if (c.getParent() != null) {
            new DropTarget(c, dropListener);
        }

        if (recursive && c instanceof Container cont) {
            // Set the container's components as listeners also
            for (Component comp : cont.getComponents()) {
                makeDropTarget(comp, true);
            }
        }

    }

    /** Determine if the dragged data is a file list. */
    private boolean isDragOk(final DropTargetDragEvent evt) {
        return stream(evt.getCurrentDataFlavors()).anyMatch((DataFlavor curFlavor) ->
                curFlavor.equals(DataFlavor.javaFileListFlavor) || curFlavor.isRepresentationClassReader());
    }

    /**
     * Removes the drag-and-drop hooks from the component and optionally from the all children.
     * You should call this if you add and remove components after you've set up the drag-and-drop.
     *
     * @param c         The component to unregister
     * @param recursive Recursively unregister components within a container
     */
    public static boolean remove(Component c, boolean recursive) {
        if (supportsDnD()) {
            c.setDropTarget(null);
            if (recursive && (c instanceof Container container)) {
                for (Component comp : container.getComponents()) {
                    remove(comp, true);
                }
                return true;
            }
        }
        return false;
    }

}
