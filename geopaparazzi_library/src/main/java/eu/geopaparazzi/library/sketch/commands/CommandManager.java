/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.geopaparazzi.library.sketch.commands;

import android.graphics.Canvas;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: almondmendoza
 * Date: 15/11/2010
 * Time: 12:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class CommandManager {
    private List<DrawingPath> currentStack;
    private List<DrawingPath> redoStack;

    /**
     *
     */
    public CommandManager() {
        currentStack = Collections.synchronizedList(new ArrayList<DrawingPath>());
        redoStack = Collections.synchronizedList(new ArrayList<DrawingPath>());
    }

    /**
     * @param command command
     */
    public void addCommand(DrawingPath command) {
        redoStack.clear();
        currentStack.add(command);
    }

    /**
     *
     */
    public void undo() {
        final int length = currentStackLength();

        if (length > 0) {
            final DrawingPath undoCommand = currentStack.get(length - 1);
            currentStack.remove(length - 1);
            undoCommand.undo();
            redoStack.add(undoCommand);
        }
    }

    /**
     * @return stack length.
     */
    public int currentStackLength() {
        final int length = currentStack.toArray().length;
        return length;
    }

    /**
     * @param canvas      canvas
     * @param doneHandler handler.
     */
    public void executeAll(Canvas canvas, Handler doneHandler) {
        if (currentStack != null) {
            synchronized (currentStack) {
                final Iterator i = currentStack.iterator();

                while (i.hasNext()) {
                    final DrawingPath drawingPath = (DrawingPath) i.next();
                    drawingPath.draw(canvas);
                    // doneHandler.sendEmptyMessage(1);
                }
            }
        }
    }

    /**
     * @return has more read.
     */
    public boolean hasMoreRedo() {
        return redoStack.toArray().length > 0;
    }

    /**
     * @return has more undo.
     */
    public boolean hasMoreUndo() {
        return currentStack.toArray().length > 0;
    }

    /**
     *
     */
    public void redo() {
        final int length = redoStack.toArray().length;
        if (length > 0) {
            final DrawingPath redoCommand = redoStack.get(length - 1);
            redoStack.remove(length - 1);
            currentStack.add(redoCommand);
        }
    }
}
