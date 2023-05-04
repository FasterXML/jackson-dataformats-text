package tools.jackson.dataformat.yaml.misc;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.dataformat.yaml.ModuleTestBase;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

// for [databind#43], deduction-based polymorphism
public class PolymorphicDeductionTest extends ModuleTestBase
{
  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class), @Type(Fleabag.class)})
  // A general supertype with no properties - used for tests involving {}
  interface Feline {}

  @JsonTypeInfo(use = DEDUCTION)
  @JsonSubTypes( {@Type(LiveCat.class), @Type(DeadCat.class)})
  // A supertype containing common properties
  public static class Cat implements Feline {
    public String name;
  }

  // Distinguished by its parent and a unique property
  static class DeadCat extends Cat {
    public String causeOfDeath;
  }

  // Distinguished by its parent and a unique property
  static class LiveCat extends Cat {
    public boolean angry;
  }

  // No distinguishing properties whatsoever
  static class Fleabag implements Feline {
    // NO OP
  }

  /*
  /**********************************************************
  /* Mock data
  /**********************************************************
   */

  private static final String DEAD_CAT_DOC = "name: Felix\ncauseOfDeath: entropy\n";
  private static final String LIVE_CAT_DOC = "name: Felix\nangry: true\n";

  /*
  /**********************************************************
  /* Test methods
  /**********************************************************
   */

  private final ObjectMapper MAPPER = newObjectMapper();

  public void testSimpleInference() throws Exception
  {
    Cat cat = MAPPER.readValue(LIVE_CAT_DOC, Cat.class);
    assertTrue(cat instanceof LiveCat);
    assertSame(cat.getClass(), LiveCat.class);
    assertEquals("Felix", cat.name);
    assertTrue(((LiveCat)cat).angry);

    cat = MAPPER.readValue(DEAD_CAT_DOC, Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("Felix", cat.name);
    assertEquals("entropy", ((DeadCat)cat).causeOfDeath);
  }

  public void testSimpleInferenceOfEmptySubtypeDoesntMatchNull() throws Exception {
    Feline feline = MAPPER.readValue("null", Feline.class);
    assertNull(feline);
  }

  public void testCaseInsensitiveInference() throws Exception
  {
    Cat cat = mapperBuilder()
      .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
      .build()
      .readValue(DEAD_CAT_DOC.toUpperCase(), Cat.class);
    assertTrue(cat instanceof DeadCat);
    assertSame(cat.getClass(), DeadCat.class);
    assertEquals("FELIX", cat.name);
    assertEquals("ENTROPY", ((DeadCat)cat).causeOfDeath);
  }
}
