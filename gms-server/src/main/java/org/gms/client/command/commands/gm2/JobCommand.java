/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package org.gms.client.command.commands.gm2;

import org.gms.client.Character;
import org.gms.client.Client;
import org.gms.client.Job;
import org.gms.client.command.Command;
import org.gms.util.I18nUtil;

public class JobCommand extends Command {
    {
        setDescription(I18nUtil.getMessage("JobCommand.message1"));
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length == 1) {
            int jobid = Integer.parseInt(params[0]);
            if (jobid < 0 || jobid >= 2200) {
                player.message(I18nUtil.getMessage("JobCommand.message2", jobid));
                return;
            }

            player.changeJob(Job.getById(jobid));
            player.equipChanged();
        } else if (params.length == 2) {
            Character victim = c.getWorldServer().getPlayerStorage().getCharacterByName(params[0]);

            if (victim != null) {
                int jobid = Integer.parseInt(params[1]);
                if (jobid < 0 || jobid >= 2200) {
                    player.message(I18nUtil.getMessage("JobCommand.message2", jobid));
                    return;
                }

                victim.changeJob(Job.getById(jobid));
                player.equipChanged();
            } else {
                player.message(I18nUtil.getMessage("BombCommand.message3", params[0]));
            }
        } else {
            player.message(I18nUtil.getMessage("JobCommand.message3"));
        }
    }
}
