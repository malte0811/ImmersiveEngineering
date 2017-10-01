package blusunrize.immersiveengineering.client.render;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.api.IEApi;
import blusunrize.immersiveengineering.common.util.IELogger;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;


public class Shaders
{
	public static int TESLA_STREAMER;
	public static void initShaders(boolean setupReload) {
		TESLA_STREAMER = createProgram(null, "/assets/"+ ImmersiveEngineering.MODID+"/shaders/tesla.frag");

		if (setupReload)
			IEApi.renderCacheClearers.add(() -> {
				if (TESLA_STREAMER!=0) {
					deleteShader(TESLA_STREAMER);
					TESLA_STREAMER = 0;
				}

				initShaders(false);
			});
	}
	//All stolen from Botania...
	private static final int VERT = ARBVertexShader.GL_VERTEX_SHADER_ARB;
	private static final int FRAG = ARBFragmentShader.GL_FRAGMENT_SHADER_ARB;


	private static void deleteShader(int id) {
		if (id != 0) {
			ARBShaderObjects.glDeleteObjectARB(id);
		}
	}

	public static void useShader(int shader) {
		ARBShaderObjects.glUseProgramObjectARB(shader);

		if(shader != 0) {
			int time = ARBShaderObjects.glGetUniformLocationARB(shader, "time");
			ARBShaderObjects.glUniform1fARB(time, Minecraft.getMinecraft().world.getTotalWorldTime()+Minecraft.getMinecraft().getRenderPartialTicks());
		}
	}

	public static void stopUsingShaders() {
		useShader(0);
	}
	private static int createProgram(String vert, String frag) {
		int vertId = 0, fragId = 0, program;
		if(vert != null)
			vertId = createShader(vert, VERT);
		if(frag != null)
			fragId = createShader(frag, FRAG);

		program = ARBShaderObjects.glCreateProgramObjectARB();
		if(program == 0)
			return 0;

		if(vert != null)
			ARBShaderObjects.glAttachObjectARB(program, vertId);
		if(frag != null)
			ARBShaderObjects.glAttachObjectARB(program, fragId);

		ARBShaderObjects.glLinkProgramARB(program);
		if(ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
			IELogger.error(getLogInfo(program));
			return 0;
		}

		ARBShaderObjects.glValidateProgramARB(program);
		if (ARBShaderObjects.glGetObjectParameteriARB(program, ARBShaderObjects.GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
			IELogger.error(getLogInfo(program));
			return 0;
		}

		return program;
	}

	private static int createShader(String filename, int shaderType){
		int shader = 0;
		try {
			shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);

			if(shader == 0)
				return 0;

			ARBShaderObjects.glShaderSourceARB(shader, readFileAsString(filename));
			ARBShaderObjects.glCompileShaderARB(shader);

			if (ARBShaderObjects.glGetObjectParameteriARB(shader, ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
				throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

			return shader;
		}
		catch(Exception e) {
			ARBShaderObjects.glDeleteObjectARB(shader);
			e.printStackTrace();
			return -1;
		}
	}

	private static String getLogInfo(int obj) {
		return ARBShaderObjects.glGetInfoLogARB(obj, ARBShaderObjects.glGetObjectParameteriARB(obj, ARBShaderObjects.GL_OBJECT_INFO_LOG_LENGTH_ARB));
	}

	private static String readFileAsString(String filename) throws Exception {
		InputStream in = Shaders.class.getResourceAsStream(filename);

		if(in == null)
			return "";

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}
}
