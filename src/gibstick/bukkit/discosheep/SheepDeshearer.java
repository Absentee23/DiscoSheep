/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gibstick.bukkit.discosheep;

import java.util.List;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShearEntityEvent;

/**
 *
 * @author Mauve
 */
public class SheepDeshearer implements Listener {

	DiscoSheep parent;

	public SheepDeshearer(DiscoSheep parent) {
		this.parent = parent;
	}

	@EventHandler
	public void onPlayerShear(PlayerShearEntityEvent e) {
		if (e.getEntity() instanceof Sheep){
			for(DiscoParty party : parent.getParties()){
				if(party.getSheep().contains((Sheep)e.getEntity())){
					e.setCancelled(true);
				}
			}
		}
	}
}