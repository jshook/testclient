/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package com.metawiring.load.core;

import javax.script.SimpleScriptContext;
import java.io.*;

/**
 * ScriptEnvBuffer provides a variant of SimpleScriptContext which captures all
 * stdin, stdout, and stderr data into diagnostic character buffers.
 * TODO: Create a diagnostic version of this class which keeps timestamps, suitable for use as the default script context
 */
public class ScriptEnvBuffer extends SimpleScriptContext {

    private DiagWriter stdoutBuffer;
    private DiagWriter stderrBuffer;
    private DiagReader stdinBuffer;

    @Override
    public Writer getWriter() {
        if (stdoutBuffer==null) {
            synchronized(this) {
                if (stdoutBuffer==null) {
                    Writer superWriter = super.getWriter();
                    stdoutBuffer = new DiagWriter(superWriter);
                }
            }
        }
        return stdoutBuffer;
    }

    @Override
    public Writer getErrorWriter() {
        if (stderrBuffer==null) {
            synchronized(this) {
                if (stderrBuffer==null) {
                    Writer superErrorWriter = super.getErrorWriter();
                    stderrBuffer = new DiagWriter(superErrorWriter);
                }
            }
        }
        return stderrBuffer;
    }

    @Override
    public Reader getReader() {
        if (stdinBuffer == null) {
            synchronized (this) {
                if (stdinBuffer == null) {
                    Reader superReader = super.getReader();
                    stdinBuffer = new DiagReader(superReader);
                }
            }
        }
        return stdinBuffer;
    }

    public String getStdinText() {
        return stdinBuffer.buffer.toString();
    }

    public String getStderrText() {
        return stderrBuffer.buffer.toString();
    }

    public String getStdoutText() {
        return stdoutBuffer.buffer.toString();
    }

    private class DiagReader extends Reader {
        Reader wrapped;
        CharArrayWriter buffer = new CharArrayWriter(0);

        public DiagReader(Reader wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            int read = wrapped.read(cbuf, off, len);
            buffer.write(cbuf, off, len);
            return read;
        }

        @Override
        public void close() throws IOException {
            wrapped.close();
            buffer.close();
        }

    }

    private class DiagWriter extends Writer {

        Writer wrapped;
        CharArrayWriter buffer = new CharArrayWriter(0);

        public DiagWriter(Writer wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            buffer.write(cbuf, off, len);
            wrapped.write(cbuf, off, len);
        }

        @Override
        public void flush() throws IOException {
            buffer.flush();
            wrapped.flush();
        }

        @Override
        public void close() throws IOException {
            buffer.close();
            wrapped.close();
        }
    }
}
