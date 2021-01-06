package com.minecraftabnormals.savageandravage.core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;

public class SRConfig {

	public static class Common {
		public final ConfigValue<Boolean> creeperExplosionsDestroyBlocks;
		public final ConfigValue<Boolean> creeperExplosionsSpawnCreepies;
		public final ConfigValue<Boolean> creepersDropSporesAfterExplosionDeath;
		public final ConfigValue<Boolean> creepieExplosionsDestroyBlocks;
		public final ConfigValue<Boolean> noBadOmenOnDeath;

		public Common(ForgeConfigSpec.Builder builder) {
			builder.push("entities");

			builder.push("creepers");
			creeperExplosionsDestroyBlocks = builder.define("Creeper explosions destroy blocks", true);
			creeperExplosionsSpawnCreepies = builder.define("Creeper explosions spawn creepies", false);
			creepersDropSporesAfterExplosionDeath = builder.define("Creepers drop Creeper Spores after they die from an explosion", true);
			creepieExplosionsDestroyBlocks = builder.define("Creepie explosions destroy blocks", false);
			builder.pop();

			builder.push("illagers");
			noBadOmenOnDeath = builder
					.comment("Illagers with banners will no longer give Bad Omen when you kill them", "Instead, you will have to place and burn the banner that they drop with flint and steel or a fire charge")
					.define("Illagers no longer give Bad Omen when killed", false);
			builder.pop();

			builder.pop();


		}
	}

	public static class Client {
		public final ConfigValue<Boolean> creepieSprout;

		public Client(ForgeConfigSpec.Builder builder) {
			builder.push("entities");
			builder.push("creepers");
			this.creepieSprout = builder.define("Creepies have a sprout on their head", true);
			builder.pop();
			builder.pop();
		}
	}

	public static final ForgeConfigSpec COMMON_SPEC;
	public static final Common COMMON;

	public static final ForgeConfigSpec CLIENT_SPEC;
	public static final Client CLIENT;

	static {
		Pair<Common, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(Common::new);
		COMMON_SPEC = commonSpecPair.getRight();
		COMMON = commonSpecPair.getLeft();

		Pair<Client, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(Client::new);
		CLIENT_SPEC = clientSpecPair.getRight();
		CLIENT = clientSpecPair.getLeft();
	}
}
