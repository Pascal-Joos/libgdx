// LZ.OutWindow

package com.badlogic.gdx.utils.compression.lz;

import edu.ucr.cs.riple.annotator.util.Nullability;
import java.io.IOException;
import javax.annotation.Nullable;

public class OutWindow {
  @Nullable byte[] _buffer;
  int _pos;
  int _windowSize = 0;
  int _streamPos;
  @Nullable java.io.OutputStream _stream;

  public void Create(int windowSize) {
    if (_buffer == null || _windowSize != windowSize) _buffer = new byte[windowSize];
    _windowSize = windowSize;
    _pos = 0;
    _streamPos = 0;
  }

  public void SetStream(java.io.OutputStream stream) throws IOException {
    ReleaseStream();
    _stream = stream;
  }

  public void ReleaseStream() throws IOException {
    Flush();
    _stream = null;
  }

  public void Init(boolean solid) {
    if (!solid) {
      _streamPos = 0;
      _pos = 0;
    }
  }

  public void Flush() throws IOException {
    if (_stream == null) return;
    int size = _pos - _streamPos;
    if (size == 0) return;
    _stream.write(_buffer, _streamPos, size);
    if (_pos >= _windowSize) _pos = 0;
    _streamPos = _pos;
  }

  public void CopyBlock(int distance, int len) throws IOException {
    if (_buffer == null) return;
    int pos = _pos - distance - 1;
    if (pos < 0) pos += _windowSize;
    for (; len != 0; len--) {
      if (pos >= _windowSize) pos = 0;
      byte[] buffer = Nullability.castToNonnull(_buffer);
      buffer[_pos++] = buffer[pos++];
      if (_pos >= _windowSize) Flush();
    }
  }

  public void PutByte(byte b) throws IOException {
    if (_buffer == null) return;
    _buffer[_pos++] = b;
    if (_pos >= _windowSize) Flush();
  }

  public byte GetByte(int distance) {
    if (_buffer == null) return 0;
    int pos = _pos - distance - 1;
    if (pos < 0) pos += _windowSize;
    return Nullability.castToNonnull(_buffer)[pos];
  }
}
