package net.sf.jetro.tests.tp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.collections.api.list.primitive.MutableLongList;
import org.eclipse.collections.impl.factory.primitive.LongLists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sf.jetro.path.JsonPath;
import net.sf.jetro.transform.Jetro;
import net.sf.jetro.transform.highlevel.TransformationSpecification;
import net.sf.jetro.tree.JsonObject;
import net.sf.jetro.tree.JsonProperty;

public class TransformationPerformanceTest {
	private static final JsonPointer ARRAY_MEMBER_POINTER = JsonPointer.compile("/members/62");
	private static final JsonPointer ARRAY_POINTER = JsonPointer.compile("/members");
	private static final JsonPath MEMBERS_AGE_PATH = JsonPath.compile("$.members[62].age");
	private static final JsonPath MEMBERS_ARRAY_END_PATH = JsonPath.compile("$.members[-]");
	
	private JsonObject jetroMember;
	private ObjectNode jacksonMember;
	private Map<String, MutableLongList> durations = new HashMap<>();
	
	@BeforeClass
	public void init() {
		jetroMember = new JsonObject();
		jetroMember.add(new JsonProperty("name", "James Bond"));
		jetroMember.add(new JsonProperty("age", 7));
		
		jacksonMember = new ObjectNode(JsonNodeFactory.instance);
		jacksonMember.put("name", "James Bond");
		jacksonMember.put("age", 7);
		
		durations.put("jetro", LongLists.mutable.empty());
		durations.put("jackson", LongLists.mutable.empty());
	}
	
	@Test(invocationCount = 10)
	public void jetroShouldTransformSmallJson() {
		long duration = runAndMeasureTime(100_000, () -> {
			Jetro.transform(getClass().getResourceAsStream("/data/small/small.json"))
			.applying(new TransformationSpecification() {
				
				@Override
				protected void specify() {
					at(MEMBERS_AGE_PATH).replaceWith(21);
					at(MEMBERS_ARRAY_END_PATH).addJsonValue(jetroMember);
				}
			}).writingTo(new ByteArrayOutputStream());
		});
		
		System.out.println("Jetro Small Json Duration: " + duration + "ms");
		durations.get("jetro").add(duration);
	}
	
	@Test(invocationCount = 10)
	public void jacksonShouldTransformSmallJson() {
		long duration = runAndMeasureTime(100_000, () -> {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode rootNode = mapper.readTree(
						getClass().getResourceAsStream("/data/small/small.json"));
				((ObjectNode) rootNode.at(ARRAY_MEMBER_POINTER)).put("age", 21);
				((ArrayNode) rootNode.at(ARRAY_POINTER)).add(jacksonMember);
				mapper.writeTree(mapper.createGenerator(new ByteArrayOutputStream()),
						rootNode);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		System.out.println("Jackson Small Json Duration: " + duration + "ms");
		durations.get("jackson").add(duration);
	}
	
	private long runAndMeasureTime(final int iterations, final Runnable runnable) {
		long start = System.nanoTime();
		
		for (int i = 0; i < iterations; i++) {
			runnable.run();
		}
		
		long end = System.nanoTime();
		return (end - start) / 1_000_000;
	}
	
	@AfterClass
	public void reportMeasurements() {
		MutableLongList jetroDurations = durations.get("jetro");
		System.out.println("Jetro durations count:   " + jetroDurations.size());
		System.out.println("Jetro durations average: " + jetroDurations.average());
		
		MutableLongList jacksonDurations = durations.get("jackson");
		System.out.println("Jackson durations count:   " + jacksonDurations.size());
		System.out.println("Jackson durations average: " + jacksonDurations.average());		
	}
}
