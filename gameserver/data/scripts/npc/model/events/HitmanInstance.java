/*
 * Copyright Mazaffaka Project (c) 2012.
 */

package npc.model.events;

import events.Hitman.Hitman;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.StringTokenizer;
import l2p.gameserver.Config;
import l2p.gameserver.database.DatabaseFactory;
import l2p.gameserver.model.Player;
import l2p.gameserver.model.instances.NpcInstance;
import l2p.gameserver.serverpackets.components.HtmlMessage;
import l2p.gameserver.templates.npc.NpcTemplate;
import l2p.gameserver.utils.ItemFunctions;
import org.apache.commons.lang3.StringUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Darvin
 * Date: 24.11.12
 * Time: 13:42
 */
public class HitmanInstance extends NpcInstance {
    private static final long serialVersionUID = 1L;

    private static final String UPDATE_KILLS_COUNT = "UPDATE event_hitman SET killsCount=? WHERE owner=?";

    private static final String HTML_INDEX = "scripts/events/Hitman/index.htm";
    private static final String HTML_ADDORDER = "scripts/events/Hitman/makeorder.htm";
    private static final String HTML_RESULT = "scripts/events/Hitman/result.htm";
    private static final String HTML_INFO = "scripts/events/Hitman/info.htm";
    private static final String HTML_DISABLED = "scripts/events/Hitman/disabled.htm";
    private static final String HTML_ORDERLIST = "scripts/events/Hitman/orderlist.htm";

    public HitmanInstance(int objectId, NpcTemplate template) {
        super(objectId, template);
    }

    @Override
    public void onBypassFeedback(Player player, String command) {
        if (!canBypassCheck(player, this)) {
            showChatWindow(player, HTML_ORDERLIST);
            return;
        }

        final StringTokenizer st = new StringTokenizer(command, " ");
        final String curCommand = st.nextToken();

        if (!Config.EVENT_HITMAN_ENABLED) {
            showChatWindow(player, HTML_DISABLED);
        } else if (curCommand.equals("makeorder"))
            player.sendPacket(makeOrder(player));
        else if (curCommand.equals("info"))
            showChatWindow(player, HTML_INFO);
        else if (curCommand.equals("index"))
            showChatWindow(player, HTML_INDEX);
        else if (curCommand.equals("openpage"))
            player.sendPacket(openPage(player, Integer.parseInt(st.nextToken())));
        else if (curCommand.equals("addorder")) {
            String target = null;
            int killscount = 0;
            int itemcount = 0;
            String itemname = null;
            if (st.countTokens() < 4) {
                sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "Проверьте правильность введенных данных." : "Check the entered data.");
                return;
            }
            try {
                target = st.nextToken();
                killscount = Integer.parseInt(st.nextToken());
                itemcount = Integer.parseInt(st.nextToken());
                itemname = StringUtils.EMPTY;
            } catch (NumberFormatException e) {
                sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "Проверьте правильность введенных данных." : "Check the entered data.");
            }

            if (st.hasMoreTokens()) {
                itemname = st.nextToken();
                while (st.hasMoreTokens())
                    itemname += " " + st.nextToken();
            }

            int respone = 0;
            respone = Hitman.addOrder(player, target, killscount, itemcount, itemname);

            switch (respone) {
                case 0:
                    sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "<center>Недостаточно предметов</center>" : "<center>Not enough items</center>");
                    break;
                case 1:
                    sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "<center>Одновременно вы можете заказать только одну цель.</center>" : "<center>You can order only one target at the same time.</center>");
                    break;
                case 2:
                    sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "<center>Цель купила новый паспорт и не может быть найдена.</center>" : "<center>Your target bought a new passport and can`t be found.</center>");
                    break;
                case 3:
                    sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "<center>Вы не можете заказать сами себя.</center>" : "<center>You can`t order yourself.</center>");
                    break;
                case 5:
                    sendResult(player, player.isLangRus() ? "Выполнено!" : "Completed!", player.isLangRus() ? "<center>Вы успешно назначили награду за голову игрока.</center>" : "<center>You have successfully nominated the award for the player's head.</center>");
                    break;
            }
        } else if (curCommand.equals("delete"))
            if (Hitman.deleteOrder(player.getObjectId()))
                sendResult(player, player.isLangRus() ? "Выполнено!" : "Completed!", player.isLangRus() ? "<center>Вы удалили свой заказ. Оставшиеся средства возвращены.</center>" : "<center>You have removed your order. The remaining funds returned.</center>");
            else
                sendResult(player, player.isLangRus() ? "Ошибка!" : "Error!", player.isLangRus() ? "<center>Вы не назначали награду.</center>" : "<center>You haven't set an award.</center>");
    }

    private HtmlMessage makeOrder(Player player) {
        final HtmlMessage html = new HtmlMessage(player, this);
        html.setFile(HTML_ADDORDER);
        html.replace("%cost%", Config.EVENT_HITMAN_COST_ITEM_COUNT + " " + ItemFunctions.createItem(Config.EVENT_HITMAN_COST_ITEM_ID).getTemplate().getName());
        html.replace("%items%", Hitman.getItemsList());
        return html;
    }

    private HtmlMessage openPage(Player player, int pageId) {
        final HtmlMessage html = new HtmlMessage(player, this);
        html.setFile(HTML_ORDERLIST);

        StringBuilder sb = new StringBuilder();

        final int count = Hitman.getOrdersCount();

        int num = pageId * Config.EVENT_HITMAN_TASKS_PER_PAGE;
        if (num > count)
            num = count;
        if (count > 0) {
            sb.append("<table width=260 border=0 cellspacing=3 cellpadding=3>");
            sb.append("<tr><td align=center valign=top>");

            for (int i = pageId * Config.PLAYERS_PER_PAGE - Config.PLAYERS_PER_PAGE; i < num; i++) {
                final Order order = Hitman.getOrderById(i);
                sb.append("<table width=250 border=1 cellspacing=5 cellpadding=5>");
                sb.append("<tr>");
                sb.append("<td align=center width=120>").append(player.isLangRus() ? "Заказчик:" : "Customer:").append("</td>");
                sb.append("<td align=center width=120><font color=\"00ff00\">").append(order.getOwner()).append("</font></td>");
                sb.append("</tr>");

                sb.append("<tr>");
                sb.append("<td align=center width=120>").append(player.isLangRus() ? "Цель:" : "Target Name:").append("</td>");
                sb.append("<td align=center width=120><font color=\"ff0000\">").append(order.getTargetName()).append("</font></td>");
                sb.append("</tr>");

                sb.append("<tr>");
                sb.append("<td align=center width=120>").append(player.isLangRus() ? "Кол-во убийств:" : "Kills Count:").append("</td>");
                sb.append("<td align=center width=120><font color=\"ff0000\">").append(order.getKillsCount()).append("</font></td>");
                sb.append("</tr>");

                sb.append("<tr>");
                sb.append("<td align=center width=120>").append(player.isLangRus() ? "Награда:" : "Reward:").append("</td>");
                sb.append("<td align=center width=120><font color=\"LEVEL\">").append(order.getItemCount()).append(" ").append(ItemFunctions.createItem(order.getItemId()).getTemplate().getName()).append("</font></td>");
                sb.append("</tr>");
                sb.append("</table><br><br><br>");
            }
            sb.append("</td></tr>");
            sb.append("</table><br><br><br>");
            int pg = getPagesCount(count);
            sb.append(player.isLangRus() ? "Страница:&nbsp;" : "Page:&nbsp;");
            for (int i = 1; i <= pg; i++)
                if (i == pageId)
                    sb.append(i).append("&nbsp;");
                else
                    sb.append("<center><button value=\"").append(i).append("\" action=\"bypass -h npc_%objectId%_openpage ").append(i).append("\" width=25 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center>&nbsp;");

        } else
            sb.append(player.isLangRus() ? "<br><center>Ставок пока не сделано</center>" : "<br><center>Bids have not yet done</center>");
        html.replace("%data%", sb.toString());
        return html;
    }

    private void sendResult(Player player, String title, String text) {
        final HtmlMessage html = new HtmlMessage(player, this);
        html.setFile(HTML_RESULT);
        html.replace("%title%", title);
        html.replace("%text%", text);
        player.sendPacket(html);
    }

    private int getPagesCount(int tasksCount) {
        if (tasksCount % Config.EVENT_HITMAN_TASKS_PER_PAGE > 0)
            return tasksCount / Config.EVENT_HITMAN_TASKS_PER_PAGE + 1;
        return tasksCount / Config.EVENT_HITMAN_TASKS_PER_PAGE;
    }

    @Override
    public void showChatWindow(Player player, int val, Object... args) {
        showChatWindow(player, HTML_INDEX);
    }

    public static class Order {
        private String target;
        private String owner;
        private int itemId;
        private int itemCount;
        private int killsCount;

        public Order(String owner, String target, int itemId, int itemcount, int killsCount) {
            this.owner = owner;
            this.target = target;
            this.itemId = itemId;
            itemCount = itemcount;
            this.killsCount = killsCount;
        }

        public String getTargetName() {
            return target;
        }

        public String getOwner() {
            return owner;
        }

        public int getItemId() {
            return itemId;
        }

        public int getItemCount() {
            return itemCount;
        }

        public int getKillsCount() {
            return killsCount;
        }

        public void decrementKillsCount() {
            --killsCount;
            Connection con = null;
            PreparedStatement statement = null;
            try {
                con = DatabaseFactory.getInstance().getConnection();
                statement = con.prepareStatement(UPDATE_KILLS_COUNT);
                statement.setInt(1, killsCount);
                statement.setString(2, owner);
                statement.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
