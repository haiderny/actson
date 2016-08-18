// MIT License
//
// Copyright (c) 2016 Michel Kraemer
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package de.undercouch.actson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.undercouch.actson.examples.pretty.PrettyPrinter;

/**
 * Tests {@link JsonParser}
 * @author Michel Kraemer
 */
public class JsonParserTest {
  /**
   * Parse a JSON string with the {@link JsonParser} and return
   * a new JSON string generated by {@link PrettyPrinter}. Assert
   * that the input JSON string is valid.
   * @param json the JSON string to parse
   * @return the new JSON string
   */
  private String parse(String json) {
    byte[] buf = json.getBytes(StandardCharsets.UTF_8);
    
    PrettyPrinter printer = new PrettyPrinter();
    JsonParser parser = new JsonParser();
    
    int i = 0;
    int event;
    do {
      while ((event = parser.nextEvent()) == JsonEvent.NEED_MORE_INPUT) {
        while (!parser.getFeeder().isFull() && i < buf.length) {
          parser.getFeeder().feed(buf[i]);
          ++i;
        }
        if (i == buf.length) {
          parser.getFeeder().done();
        }
      }
      assertFalse("Invalid JSON text", event == JsonEvent.ERROR);
      printer.onEvent(event, parser);
    } while (event != JsonEvent.EOF);
    
    return printer.getResult();
  }

  /**
   * Assert that two JSON objects are equal
   * @param expected the expected JSON object
   * @param actual the actual JSON object
   */
  private static void assertJsonObjectEquals(String expected, String actual) {
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<Map<String, Object>> ref =
        new TypeReference<Map<String, Object>>() { };
    try {
      Map<String, Object> em = mapper.readValue(expected, ref);
      Map<String, Object> am = mapper.readValue(actual, ref);
      assertEquals(em, am);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Assert that two JSON arrays are equal
   * @param expected the expected JSON array
   * @param actual the actual JSON array
   */
  private static void assertJsonArrayEquals(String expected, String actual) {
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<List<Object>> ref = new TypeReference<List<Object>>() { };
    try {
      List<Object> el = mapper.readValue(expected, ref);
      List<Object> al = mapper.readValue(actual, ref);
      assertEquals(el, al);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Test if valid files can be parsed correctly
   * @throws IOException if one of the test files could not be read
   */
  @Test
  public void testPass() throws IOException {
    for (int i = 1; i <= 3; ++i) {
      URL u = getClass().getResource("pass" + i + ".txt");
      String json = IOUtils.toString(u, "UTF-8");
      if (json.startsWith("{")) {
        assertJsonObjectEquals(json, parse(json));
      } else {
        assertJsonArrayEquals(json, parse(json));
      }
    }
  }
  
  /**
   * Test if invalid files cannot be parsed
   * @throws IOException if one of the test files could not be read
   */
  @Test
  public void testFail() throws IOException {
    for (int i = 1; i <= 33; ++i) {
      URL u = getClass().getResource("fail" + i + ".txt");
      byte[] json = IOUtils.toByteArray(u);
      JsonParser parser;
      
      if (i == 18) {
        // test for too many nested modes
        parser = new JsonParser(16);
      } else {
        parser = new JsonParser();
      }
      
      boolean ok = true;
      int j = 0;
      int event;
      do {
        while ((event = parser.nextEvent()) == JsonEvent.NEED_MORE_INPUT) {
          while (!parser.getFeeder().isFull() && j < json.length) {
            parser.getFeeder().feed(json[j]);
            ++j;
          }
          if (j == json.length) {
            parser.getFeeder().done();
          }
        }
        ok &= (event != JsonEvent.ERROR);
      } while (ok && event != JsonEvent.EOF);
      
      assertFalse(ok);
    }
  }

  /**
   * Test if an empty object is parsed correctly
   */
  @Test
  public void emptyObject() {
    String json = "{}";
    assertJsonObjectEquals(json, parse(json));
  }

  /**
   * Test if an object with one property is parsed correctly
   */
  @Test
  public void simpleObject() {
    String json = "{\"name\": \"Elvis\"}";
    assertJsonObjectEquals(json, parse(json));
  }

  /**
   * Test if an empty array is parsed correctly
   */
  @Test
  public void emptyArray() {
    String json = "[]";
    assertJsonArrayEquals(json, parse(json));
  }

  /**
   * Test if a simple array is parsed correctly
   */
  @Test
  public void simpleArray() {
    String json = "[\"Elvis\", \"Max\"]";
    assertJsonArrayEquals(json, parse(json));
  }

  /**
   * Test if an array with mixed values is parsed correctly
   */
  @Test
  public void mixedArray() {
    String json = "[\"Elvis\", 132, \"Max\", 80.67]";
    assertJsonArrayEquals(json, parse(json));
  }
}
