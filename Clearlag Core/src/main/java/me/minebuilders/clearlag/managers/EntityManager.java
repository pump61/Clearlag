package me.minebuilders.clearlag.managers;

import me.minebuilders.clearlag.Clearlag;
import me.minebuilders.clearlag.annotations.ConfigValue;
import me.minebuilders.clearlag.events.EntityRemoveEvent;
import me.minebuilders.clearlag.modules.ClearModule;
import me.minebuilders.clearlag.modules.ClearlagModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EntityManager extends ClearlagModule {

    @ConfigValue(path = "settings.enable-api")
	private final boolean enabled = true;

	public int removeEntities(ClearModule mod) {
		int removed = 0;

		for (World w : Bukkit.getWorlds()) {
			removed += removeEntities(mod.getRemovables(w.getEntities(), w), w);
		}

		return removed;
	}

	public int removeEntities(List<Entity> removables, World w) {

		EntityRemoveEvent et = new EntityRemoveEvent(removables, w);

		if (enabled)
			Bukkit.getPluginManager().callEvent(et);

		// ✅ relatório em arquivo (não no console, só log) do que está sendo
		// removido em cada limpeza (manual /lagg clear OU a automática). Pedido
		// depois de um caso real (2026-09-09) onde 57 mil itens se acumularam
		// no chão e derrubaram o TPS do servidor pra 11-12, sem nenhum jeito de
		// saber depois o que exatamente tinha sido limpo, já que o comando só
		// informa o total e não um detalhamento por tipo/material.
		logRemovalReport(et.getEntityList(), w);

		for (Entity en : et.getEntityList())
			en.remove();

		return et.getEntityList().size();
	}

	private void logRemovalReport(List<Entity> entities, World w) {
		if (entities.isEmpty()) {
			return;
		}

		Map<String, Integer> tally = new TreeMap<>();
		for (Entity en : entities) {
			String key;
			if (en instanceof Item) {
				key = "ITEM:" + ((Item) en).getItemStack().getType().name();
			} else {
				key = en.getType().name();
			}
			tally.merge(key, 1, Integer::sum);
		}

		try {
			Path logFile = Clearlag.getInstance().getDataFolder().toPath().resolve("clear-reports.log");
			Files.createDirectories(logFile.getParent());

			try (PrintWriter pw = new PrintWriter(new FileWriter(logFile.toFile(), true))) {
				String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
				pw.println("[" + timestamp + "] mundo=" + w.getName() + " total=" + entities.size());
				for (Map.Entry<String, Integer> e : tally.entrySet()) {
					pw.println("    " + e.getKey() + " x" + e.getValue());
				}
			}
		} catch (IOException ex) {
			Clearlag.getInstance().getLogger().warning("[ClearLag] Falha ao escrever clear-reports.log: " + ex.getMessage());
		}
	}
}
