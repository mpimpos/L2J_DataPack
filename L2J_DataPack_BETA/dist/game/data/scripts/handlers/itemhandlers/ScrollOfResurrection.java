/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.itemhandlers;

import com.l2jserver.gameserver.datatables.SkillTable;
import com.l2jserver.gameserver.handler.IItemHandler;
import com.l2jserver.gameserver.instancemanager.CastleManager;
import com.l2jserver.gameserver.model.actor.L2Character;
import com.l2jserver.gameserver.model.actor.L2Playable;
import com.l2jserver.gameserver.model.actor.instance.L2PcInstance;
import com.l2jserver.gameserver.model.actor.instance.L2PetInstance;
import com.l2jserver.gameserver.model.entity.Castle;
import com.l2jserver.gameserver.model.entity.TvTEvent;
import com.l2jserver.gameserver.model.items.instance.L2ItemInstance;
import com.l2jserver.gameserver.model.skills.L2Skill;
import com.l2jserver.gameserver.network.SystemMessageId;
import com.l2jserver.gameserver.network.serverpackets.ActionFailed;
import com.l2jserver.gameserver.network.serverpackets.SystemMessage;

public class ScrollOfResurrection implements IItemHandler
{
	@Override
	public boolean useItem(L2Playable playable, L2ItemInstance item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}
		
		if (!TvTEvent.onScrollUse(playable.getObjectId()))
		{
			playable.sendPacket(ActionFailed.STATIC_PACKET);
			return false;
		}
		
		final L2PcInstance activeChar = playable.getActingPlayer();
		if (activeChar.isSitting())
		{
			activeChar.sendPacket(SystemMessageId.CANT_MOVE_SITTING);
			return false;
		}
		
		if (activeChar.isMovementDisabled())
		{
			return false;
		}
		
		final int itemId = item.getItemId();
		// boolean blessedScroll = (itemId != 737);
		boolean petScroll = (itemId == 6387);
		
		// SoR Animation section
		final L2Character target = (L2Character) activeChar.getTarget();
		if ((target == null) || !target.isDead())
		{
			activeChar.sendPacket(SystemMessageId.INCORRECT_TARGET);
			return false;
		}
		
		L2PcInstance targetPlayer = null;
		if (target instanceof L2PcInstance)
		{
			targetPlayer = (L2PcInstance) target;
		}
		
		L2PetInstance targetPet = null;
		if (target instanceof L2PetInstance)
		{
			targetPet = (L2PetInstance) target;
		}
		
		if ((targetPlayer != null) || (targetPet != null))
		{
			boolean condGood = true;
			
			// check target is not in a active siege zone
			Castle castle = null;
			
			if (targetPlayer != null)
			{
				castle = CastleManager.getInstance().getCastle(targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ());
			}
			else
			{
				castle = CastleManager.getInstance().getCastle(targetPet.getOwner().getX(), targetPet.getOwner().getY(), targetPet.getOwner().getZ());
			}
			
			if ((castle != null) && castle.getSiege().getIsInProgress())
			{
				condGood = false;
				activeChar.sendPacket(SystemMessageId.CANNOT_BE_RESURRECTED_DURING_SIEGE);
			}
			
			if (targetPet != null)
			{
				if (targetPet.getOwner() != activeChar)
				{
					if (targetPet.getOwner().isReviveRequested())
					{
						if (targetPet.getOwner().isRevivingPet())
						{
							activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
						}
						else
						{
							activeChar.sendPacket(SystemMessageId.CANNOT_RES_PET2); // A pet cannot be resurrected while it's owner is in the process of resurrecting.
						}
						condGood = false;
					}
				}
			}
			else
			{
				if (targetPlayer.isFestivalParticipant()) // Check to see if the current player target is in a festival.
				{
					condGood = false;
					activeChar.sendMessage("You may not resurrect participants in a festival.");
				}
				if (targetPlayer.isReviveRequested())
				{
					if (targetPlayer.isRevivingPet())
					{
						activeChar.sendPacket(SystemMessageId.MASTER_CANNOT_RES); // While a pet is attempting to resurrect, it cannot help in resurrecting its master.
					}
					else
					{
						activeChar.sendPacket(SystemMessageId.RES_HAS_ALREADY_BEEN_PROPOSED); // Resurrection is already been proposed.
					}
					condGood = false;
				}
				else if (petScroll)
				{
					condGood = false;
					activeChar.sendMessage("You do not have the correct scroll");
				}
			}
			
			if (condGood)
			{
				if (!activeChar.destroyItem("Consume", item.getObjectId(), 1, null, false))
				{
					return false;
				}
				
				int skillId = 0;
				int skillLevel = 1;
				
				switch (itemId)
				{
					case 737:
						skillId = 2014;
						break; // Scroll of Resurrection
					case 3936:
						skillId = 2049;
						break; // Blessed Scroll of Resurrection
					case 3959:
						skillId = 2062;
						break; // L2Day - Blessed Scroll of Resurrection
					case 6387:
						skillId = 2179;
						break; // Blessed Scroll of Resurrection: For Pets
					case 9157:
						skillId = 2321;
						break; // Blessed Scroll of Resurrection Event
					case 10150:
						skillId = 2393;
						break; // Blessed Scroll of Battlefield Resurrection
					case 13259:
						skillId = 2596;
						break; // Gran Kain's Blessed Scroll of Resurrection
				}
				
				if (skillId != 0)
				{
					L2Skill skill = SkillTable.getInstance().getInfo(skillId, skillLevel);
					activeChar.useMagic(skill, true, true);
					
					SystemMessage sm = SystemMessage.getSystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(item);
					activeChar.sendPacket(sm);
				}
				return true;
			}
		}
		return false;
	}
}
