package org.gms.service;

import com.mybatisflex.core.query.QueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.gms.client.DefaultDates;
import org.gms.config.YamlConfig;
import org.gms.dao.entity.AccountsDO;
import org.gms.dao.entity.BuddiesDO;
import org.gms.dao.entity.CharactersDO;
import org.gms.dao.entity.WorldtransfersDO;
import org.gms.dao.mapper.BuddiesMapper;
import org.gms.dao.mapper.CharactersMapper;
import org.gms.dao.mapper.WorldtransfersMapper;
import org.gms.manager.ServerManager;
import org.gms.net.server.Server;
import org.gms.util.I18nUtil;
import org.gms.util.RequireUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

import static org.gms.dao.entity.table.CharactersDOTableDef.CHARACTERS_D_O;
import static org.gms.dao.entity.table.WorldtransfersDOTableDef.WORLDTRANSFERS_D_O;

@Service
@AllArgsConstructor
@Slf4j
public class WorldTransferService {
    private final WorldtransfersMapper worldtransfersMapper;
    private final CharactersMapper charactersMapper;
    private final AccountService accountService;
    private final BuddiesMapper buddiesMapper;

    public void applyAllWorldTransfer() {
        List<WorldtransfersDO> worldtransfersDOList = worldtransfersMapper.selectListByQuery(QueryWrapper.create()
                .where(WORLDTRANSFERS_D_O.COMPLETION_TIME.isNull()));
        worldtransfersDOList.forEach(worldtransfersDO -> {
            try {
                if (checkWorldTransferEligibility(worldtransfersDO)) {
                    ServerManager.getApplicationContext().getBean(WorldTransferService.class).doWorldTransfer(worldtransfersDO);
                }
            } catch (Exception e) {
                log.error(I18nUtil.getLogMessage("Server.init.error5"), e);
            }
        });
    }

    /**
     * 校验能否转区
     *
     * @param data 转区对象
     * @return 能否转区
     */
    public boolean checkWorldTransferEligibility(WorldtransfersDO data) {
        if (!YamlConfig.config.server.ALLOW_CASHSHOP_WORLD_TRANSFER) {
            return false;
        }
        // 获取人物信息
        CharactersDO charactersDO = charactersMapper.selectOneById(data.getCharacterid());
        if (charactersDO == null) {
            return false;
        }
        // 判断是否结婚
        if (charactersDO.getPartnerId() != null) {
            return false;
        }
        // 判断是否被封禁
        AccountsDO accountsDO = accountService.findById(charactersDO.getAccountid());
        if (accountsDO == null) {
            return false;
        }
        if (accountsDO.getBanned() != null && accountsDO.getBanned()) {
            return false;
        }
        if (accountsDO.getTempban() != null && !Objects.equals(accountsDO.getTempban().toLocalDateTime(), DefaultDates.getTempban())) {
            return false;
        }
        // 判断名字是否被占用
        long count = charactersMapper.selectCountByQuery(QueryWrapper.create()
                .where(CHARACTERS_D_O.NAME.eq(charactersDO.getName()))
                .and(CHARACTERS_D_O.WORLD.eq(data.getTo())));
        if (count > 0) {
            return false;
        }
        // 判断大区是否存在，万一被删了
        return Server.getInstance().getWorld(data.getTo()) != null;
    }

    /**
     * 转区
     *
     * @param data 转区对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void doWorldTransfer(WorldtransfersDO data) {
        // 获取人物信息
        CharactersDO charactersDO = charactersMapper.selectOneById(data.getCharacterid());
        RequireUtil.requireNotNull(charactersDO, I18nUtil.getLogMessage("UNKNOWN_CHARACTER"));
        charactersMapper.update(CharactersDO.builder()
                .id(charactersDO.getId())
                .world(data.getTo())
                .meso(Math.min(charactersDO.getMeso(), 1000000))
                .guildid(0L)
                .guildrank(0L)
                .build());
        buddiesMapper.delete(BuddiesDO.builder().characterid(charactersDO.getId()).build());
        buddiesMapper.delete(BuddiesDO.builder().buddyid(charactersDO.getId()).build());
        worldtransfersMapper.update(WorldtransfersDO.builder().id(data.getId()).completionTime(new Timestamp(System.currentTimeMillis())).build());
        log.info(I18nUtil.getLogMessage("CharacterService.doWorldTransfer.info1"), data.getFrom(), data.getTo());
    }
}