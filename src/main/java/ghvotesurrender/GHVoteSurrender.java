package ghvotesurrender;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.util.*;
import io.anuke.mindustry.core.NetClient;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.plugin.Plugin;

import static io.anuke.arc.util.Log.info;
import static io.anuke.mindustry.Vars.*;

public class GHVoteSurrender extends Plugin{

    private VoteSession voting = null;
    private long cooldown = -1;
    private String cmd = "ghvs";
    private String description = "Vote for Surrender.";
    private static int VOTE_ID = -1;

    public void registerServerCommands(CommandHandler handler){
        handler.register(cmd, "[on/off|y/n]", description, args -> {
            if (args.length == 0)
                Log.info(!mode() ? "The Plugin is Not Turned On Yet. Do '" + cmd + " on' to turn on." :
                        voting == null ? "No Voting Session is being hosted right now. Use '" + cmd + " y' or '" + cmd + " n' to start a new voting session." :
                                voting.status());
            else switch (args[0]) {
                case "vr":
                    try{
                        if(args.length < 2) {
                            Log.info("Current Vote Requirement is " + votesRequired() + "%.");
                            break;
                        }
                        float f = Float.valueOf(args[1]);
                        votesRequired(f);
                    }catch (NumberFormatException e){
                        Log.info("args[0] is not float. Try again.");
                        break;
                    }
                    break;
                case "on": case "off":
                    mode(args[0].equals("on"));
                    break;
                case "y": case "n":
                    if (!mode()) return;
                    if (voting == null) newVote();
                    if(state.gameOver){
                        player.sendMessage("[lightgray]Just surrendered, please wait a little bit first.");
                        return;
                    }
                    voting.vote(null, args[0].equals("y"));
                    break;
                case "help":
                    Log.info("GHVoteSurrender: \n" +
                            "'" + cmd + " on/off' to Turn On/Off the Plugin.\n" +
                            "'" + cmd + " y/n' to (Host&)Vote.\n" +
                            "'" + cmd + " vr' to Set/Get the Vote Requirement(in '%' form).\n" +
                            "Notes: 2 x 'y' = -1 x 'n'. 2 Votes on Yes are equivalent to Only 1 Vote on No.\n" +
                            "A Vote is Passed When There are Over " + votesRequired() + "%.");
                    break;
            }
        });
    }

    public void registerClientCommands(CommandHandler handler){
        handler.<Player>register(cmd, "[y/n]", description, (args, player) -> {
            if(args.length == 0)
                player.sendMessage("[lightgray][orange]" + (!mode() ? "[orange]The Plugin is Not Turned On Yet." : "") +
                        (voting == null ? "No Voting Session is being hosted right now. Use '" + cmd + " y' or '" + cmd + " n' to start a new voting session." :
                                voting.status()));
            else switch (args[0]) {
                case "on": case "off":
                    if(player.isAdmin) mode(args[0].equals("on"));
                    break;
                case "y": case "n":
                    if (!mode()) return;
                    if (System.currentTimeMillis() <= cooldown) {
                        player.sendMessage("[lightgray]Penalty is on right now, please wait for the 5 mins to pass.");
                        return;
                    }else if(state.gameOver){
                        player.sendMessage("[lightgray]This game is already over.");
                        return;
                    }
                    if (voting == null) newVote();
                    if (voting.voted.contains(player.uuid) || voting.voted.contains(netServer.admins.getInfo(player.uuid).lastIP))
                        player.sendMessage("[lightgray]You have Voted already.");
                    else
                        voting.vote(player, args[0].equals("y"));
                    break;
                case "help":
                    player.sendMessage("[lightgray][orange]GHVoteSurrender[]: \n" +
                            (player.isAdmin ? "'[orange]/" + cmd + " on/off'[] to Turn On/Off the Plugin.\n" : "") +
                            "[orange]'/" + cmd + " y/n'[] to (Host a )Vote.\n" +
                            "Notes: 2 * [orange]'[green]yes[]'[] = -1 * [orange]'[red]no[]'[]. 2 Votes on [green]Yes[] are equivalent to 1 Vote on [red]No[] Only.\n" +
                            "A Vote is Passed When There are Over " + votesRequired() + "%.");
                    break;
            }
        });
    }

    private void newVote(){
        VOTE_ID++;
        voting = new VoteSession(VOTE_ID);
    }
    private void mode(boolean set){
        Core.settings.put("ghvotesurrendermode", set);
    }
    private boolean mode(){
        return Core.settings.getBool("ghvotesurrendermode", true);
    }
    private void votesRequired(float set){
        Core.settings.put("ghvotesurrendervotesrequired", set);
    }
    private float votesRequired(){
        return Core.settings.getFloat("ghvotesurrendervotesrequired", 0.5f);
    }

    class VoteSession{
        int y, n;
        ObjectSet<String> voted = new ObjectSet<>();
        Timer.Task task;
        long start;
        float duration = 300;
        int id;

        VoteSession(int id){
            y = n = 0;
            this.task = Timer.schedule(() -> {
                if(voting != null && voting.id == VOTE_ID && !checkPass()){
                    Call.sendMessage(Strings.format("[lightgray]Vote failed. Not enough votes to gameover. Penalty: 5 mins vote cooldown."));
                    voting = null;
                    cooldown = System.currentTimeMillis() + 5 * 60 * 1000;
                }
            }, duration);
            this.start = System.currentTimeMillis();
            this.id = id;
        }

        void vote(Player player, boolean agree){
            boolean console = player == null;
            int amount = (player == null ? 3 : player.isAdmin ? 2 : 1);
            if(agree) y += amount; else n += amount * 2;
            if(!console) voted.addAll(player.uuid, netServer.admins.getInfo(player.uuid).lastIP);
            Call.sendMessage("[orange]" + (console ? "[scarlet][server]" : NetClient.colorizeName(player.id, player.name)) + "[lightgray] has voted for" + (agree ? "" : " not to") + " Surrender. \n" +
                    status() + "\n[lightgray]Type[orange] /vote <y/n>[] to agree/disagree.");
            checkPass();
        }

        boolean checkPass(){
            if(currentVotes() >= votesRequired()){
                Call.sendMessage("[green]Vote for Surrender is Passed. Surrender in 5s.");
                Timer.schedule(() -> {
                    info("&lyVoted Surrender...\n" + playerGroup.all().toString("\n"));
                    Events.fire(new EventType.GameOverEvent(Team.crux));
                    },5);
                cooldown = System.currentTimeMillis() + 1000;
                voting = null;
                return true;
            }
            return false;
        }

        float currentVotes(){
            return (y - n) / playerGroup.all().size;
        }
        int votesToPass(){
            return passed() ? 0 : (int)Math.ceil((votesRequired() - currentVotes()) * playerGroup.all().size);
        }
        boolean passed(){
            return currentVotes() >= votesRequired();
        }

        String status(){
            return "[lightgray]Currently at: [orange]" + currentVotes() * 100 + "%[] or [orange]" + y + " [green]yes[], " + n + " [red]no[][]" +
                    "\nVotes Required: [orange]" + votesRequired() * 100 + "%[], " + "or [orange]" + votesToPass() + "[] more [green]yes[]" +
                    (passed() ? "" : "\nTime Left: [orange]" + Math.round(((int)(duration - (System.currentTimeMillis() - start) / 1000f))*10)/10f + "s[]");
        }
    }
}