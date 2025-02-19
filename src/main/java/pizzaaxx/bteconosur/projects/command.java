package pizzaaxx.bteconosur.projects;

import com.google.common.collect.Lists;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.selector.Polygonal2DRegionSelector;
import com.sk89q.worldedit.world.World;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.md_5.bungee.api.chat.BaseComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pizzaaxx.bteconosur.ServerPlayer;
import pizzaaxx.bteconosur.coords.Coords2D;
import pizzaaxx.bteconosur.country.Country;
import pizzaaxx.bteconosur.playerData.PlayerData;
import pizzaaxx.bteconosur.worldedit.methods;
import pizzaaxx.bteconosur.yaml.YamlManager;
import xyz.upperlevel.spigot.book.BookUtil;

import java.awt.Color;
import java.util.*;
import java.util.List;

import static pizzaaxx.bteconosur.Config.*;
import static pizzaaxx.bteconosur.bteConoSur.*;
import static pizzaaxx.bteconosur.discord.bot.conoSurBot;
import static pizzaaxx.bteconosur.misc.misc.*;
import static pizzaaxx.bteconosur.points.PlayerPoints.pointsPrefix;
import static pizzaaxx.bteconosur.worldedit.methods.getSelection;

public class command implements CommandExecutor {
    public static String projectsPrefix = "§f[§dPROYECTO§f] §7>>§r ";
    public Set<Player> transferConfirmation = new HashSet<>();
    public Set<Player> leaveConfirmation = new HashSet<>();
    public Set<Player> finishConfirmation = new HashSet<>();
    public Set<Player> deleteConfirmation = new HashSet<>();
    public static ItemStack background;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equals("project")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(projectsPrefix + "Este comando solo puede ser usado por jugadores.");
            }
            Player p = (Player) sender;
            ServerPlayer s = new ServerPlayer(p);
            if (args.length == 0) {
                sender.sendMessage(projectsPrefix + "Debes introducir un subcomando.");
                return true;
            }

            if (args[0].equals("create") || args[0].equals("crear")) {
                Region region = null;
                try {
                    region = getSelection(p);
                } catch (IncompleteRegionException e) {
                    p.sendMessage(projectsPrefix + "§cSelecciona un área primero.");
                    return true;
                }

                // GET POINTS

                List<BlockVector2D> points = new ArrayList<>();

                if (region instanceof CuboidRegion) {
                    CuboidRegion cuboidRegion = (CuboidRegion) region;
                    Vector first = cuboidRegion.getPos1();
                    Vector second = cuboidRegion.getPos2();

                    points.add(new BlockVector2D(first.getX(), first.getZ()));
                    points.add(new BlockVector2D(second.getX(), first.getZ()));
                    points.add(new BlockVector2D(second.getX(), second.getZ()));
                    points.add(new BlockVector2D(first.getX(), second.getZ()));
                } else if (region instanceof Polygonal2DRegion) {
                    points = ((Polygonal2DRegion) region).getPoints();
                } else {
                    p.sendMessage(projectsPrefix + "Debes seleccionar una region cúbica o poligonal.");
                    return true;
                }

                if (points.size() < 3) {
                    p.sendMessage(projectsPrefix + "Selecciona un área primero.");
                    return true;
                }

                if (points.size() > maxProjectPoints) {
                    p.sendMessage(projectsPrefix + "La selección no puede tener más de 15 puntos.");
                    return true;
                }

                Country country = new Country(points.get(0));

                if (country.getCountry().equals("global")) {
                    p.sendMessage(projectsPrefix + "Los proyectos no funcionan aquí.");
                    return true;
                }

                if (p.hasPermission("bteconosur.projects.manage.create")) {

                    if (!s.getPermissionCountries().contains(country.getCountry())) {
                        p.sendMessage(projectsPrefix + "No puedes hacer esto aquí.");
                    }

                    if (args.length < 2) {
                        p.sendMessage(projectsPrefix + "Introduce una dificultad, puede ser §afacil§f, §aintermedio§f o §adificil§f.");
                        return true;
                    }

                    if ((!(args[1].equals("facil"))) && (!(args[1].equals("intermedio"))) && (!(args[1].equals("dificil")))) {
                        p.sendMessage(projectsPrefix + "Introduce una dificultad válida, puede ser §afacil§f, §aintermedio§f o §adificil§f.");
                        return true;
                    }

                    Project project = new Project(getCountryAtLocation(new Location(mainWorld, points.get(0).getX(), 100 , points.get(0).getZ())), args[1], points);

                    if (!(s.getPermissionCountries().contains(project.getCountry()))) {
                        p.sendMessage(projectsPrefix + "No puedes hacer esto aquí.");
                        return true;
                    }

                    project.upload();
                    // SEND MESSAGES

                    p.sendMessage(projectsPrefix + "Proyecto con la ID §a" + project.getId()  + "§f creado con la dificultad §a" + project.getDifficulty().toUpperCase() + "§f.");

                    String dscMessage = ":clipboard: **" + p.getName() + "** ha creado el proyecto `" + project.getId() + "` con dificultad `" + args[1].toUpperCase() + "` en las coordenadas: \n";
                    for (BlockVector2D point : project.getPoints()) {
                        dscMessage = dscMessage + "> " + Math.floor(point.getX()) + " " + Math.floor(p.getWorld().getHighestBlockAt(point.getBlockX(), point.getBlockZ()).getY()) + " " + Math.floor(point.getZ()) + "\n";
                    }
                    dscMessage = dscMessage.replace(".0", "");

                    getLogsChannel(project.getCountry()).sendMessage(dscMessage).queue();
                    return true;
                } else if (p.hasPermission("bteconosur.projects.create")) {
                    if (new ServerPlayer(p).getProjects().size() < maxProjectsPerPlayer) {
                        Project project = new Project(getCountryAtLocation(new Location(mainWorld, points.get(0).getX(), 100 , points.get(0).getZ())), "facil", points);


                        String channelId;
                        if (project.getCountry().equals("argentina")) {
                            channelId = "932074847016718426";
                        } else if (project.getCountry().equals("bolivia")) {
                            channelId = "932074847016718426";
                        } else if (project.getCountry().equals("chile")) {
                            channelId = "932074847016718426";
                        } else if (project.getCountry().equals("peru")) {
                            channelId = "932074847016718426";
                        } else {
                            p.sendMessage(projectsPrefix + "Los proyectos no funcionan aquí.");
                            return true;
                        }

                        EmbedBuilder request = new EmbedBuilder();
                        request.setColor(new Color(0, 255, 42));
                        request.setTitle(new ServerPlayer(p).getName() + " quiere crear un proyecto.");

                        List<String> coords = new ArrayList<>();
                        for (BlockVector2D point : project.getPoints()) {
                            coords.add(("> " + point.getX() + " " + new Coords2D(point).getHighestY() + " " + point.getZ()).replace(".0", ""));
                        }
                        request.addField(":round_pushpin: Coordenadas:", String.join("\n", coords), false);

                        // GMAPS

                        Double minX = project.getPoints().get(0).getX();
                        Double maxX = project.getPoints().get(0).getX();
                        Double minZ = project.getPoints().get(0).getZ();
                        Double maxZ = project.getPoints().get(0).getZ();

                        for (BlockVector2D point : project.getPoints()) {
                            if (point.getX() > maxX) {
                                maxX = point.getX();
                            }
                            if (point.getX() < minX) {
                                minX = point.getX();
                            }
                            if (point.getZ() > maxZ) {
                                maxZ = point.getZ();
                            }
                            if (point.getZ() < minZ) {
                                minZ = point.getZ();
                            }
                        }

                        Coords2D geoCoord = new Coords2D(new Location(mainWorld, (minX + maxX) / 2, 100, (minZ + maxZ) / 2));

                        request.addField(":map: Google Maps:", "https://www.google.com/maps/@" + geoCoord.getLat() + "," + geoCoord.getLon() + ",19z", false);

                        // IMAGE

                        request.setImage(project.getImageUrl());

                        ActionRow actionRow = ActionRow.of(
                                Button.of(ButtonStyle.SECONDARY, "facil", "Fácil", Emoji.fromMarkdown("\uD83D\uDFE2")),
                                Button.of(ButtonStyle.SECONDARY, "intermedio", "Intermedio", Emoji.fromMarkdown("\uD83D\uDFE1")),
                                Button.of(ButtonStyle.SECONDARY, "dificil", "Difícil", Emoji.fromMarkdown("\uD83D\uDD34")),
                                Button.of(ButtonStyle.DANGER, "rechazar", "Rechazar", Emoji.fromMarkdown("✖️"))
                        );

                        MessageBuilder message = new MessageBuilder();
                        message.setEmbeds(request.build());
                        message.setActionRows(actionRow);

                        conoSurBot.getTextChannelById(channelId).sendMessage(message.build()).queue();

                        p.sendMessage(projectsPrefix + "Se ha enviado una solicitud para crear tu proyecto.");
                    } else {
                        p.sendMessage(projectsPrefix + "No puedes ser líder de más de 10 proyectos.");
                    }
                } else {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                }
            }

            if (args[0].equals("claim") || args[0].equals("reclamar")) {
                if (!(p.hasPermission("bteconosur.projects.claim"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                Project project;
                try {
                    project = new Project(p.getLocation());
                    if (project.getOwner() != null) {
                        if (project.getOwner() == p) {
                            p.sendMessage(projectsPrefix + "Ya eres dueñ@ de este proyecto.");
                        } else if (project.getMembers().contains(p)) {
                            p.sendMessage("Alguien más ya es dueñ@ de este proyecto. Usa §a/p request §fpara solicitar unirte.");
                        }
                    } else if (new ServerPlayer(p).getOwnedProjects().size() >= maxProjectsPerPlayer){
                        p.sendMessage(projectsPrefix + "No puedes ser líder de más de 10 proyectos al mismo tiempo.");
                    } else {
                        if ((new ServerPlayer(p).getPrimaryGroup().equals("postulante") || new ServerPlayer(p).getPrimaryGroup().equals("default")) && !(project.getDifficulty().equals("facil"))) {
                            p.sendMessage(projectsPrefix + "En tu rango solo puedes reclamar proyectos fáciles.");
                            return true;
                        }
                        project.setOwner(p);
                        project.upload();

                        p.sendMessage(projectsPrefix + "Ahora eres dueñ@ de este proyecto.");

                        getLogsChannel(project.getCountry()).sendMessage(":inbox_tray: **" + s.getName() + "** ha reclamado el proyecto `" + project.getId() + "`.").queue();
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                    return true;
                }
                return true;
            }

            if (args[0].equals("delete") || args[0].equals("eliminar")) {
                if (p.hasPermission("bteconosur.projects.manage.delete")) {
                    if (!(deleteConfirmation.contains(p))) {
                        deleteConfirmation.add(p);
                        p.sendMessage(projectsPrefix + "§cNo puedes deshacer esta acción. §fUsa el comando de nuevo para confirmar.");
                        return true;
                    }

                    deleteConfirmation.remove(p);
                    if (args.length >= 2) {
                        try {
                            Project project = new Project(args[1]);
                            project.delete();

                            p.sendMessage(projectsPrefix + "Has eliminado el proyecto §a" + project.getId() + "§f.");

                            for (OfflinePlayer member : project.getAllMembers()) {
                                new ServerPlayer(member).sendNotification(projectsPrefix + "Tu proyecto **§a" + project.getName(true) + "§f** ha sido eliminado.");
                            }

                            getLogsChannel(project.getCountry()).sendMessage(":wastebasket: **" + s.getName() + "** ha eliminado el proyecto `" + project.getId() + "`.").queue();
                        } catch (Exception e) {
                            p.sendMessage(projectsPrefix + "Este proyecto no existe.");
                            return true;
                        }
                    } else {
                        try {
                            Project project = new Project(p.getLocation());
                            project.delete();

                            p.sendMessage(projectsPrefix + "Has eliminado el proyecto §a" + project.getId() + "§f.");
                            getLogsChannel(project.getCountry()).sendMessage(":wastebasket: **" + s.getName() + "** ha eliminado el proyecto `" + project.getId() + "`.").queue();
                        } catch (Exception e) {
                            p.sendMessage(projectsPrefix + "No estás dentro de ningun proyecto.");
                        }
                    }
                } else {
                    p.sendMessage(projectsPrefix + "§cNo puedes hacer esto.");
                }
                return true;
            }

            if (args[0].equals("add") || args[0].equals("agregar")) {
                if (!(p.hasPermission("bteconosur.projects.add"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                Project project = null;
                try {
                    project = new Project(p.getLocation());
                    if (project.getOwner() == p) {
                        if (project.isPending()) {
                            p.sendMessage("No puedes hacer esto mientras el proyecto está pendiente de revisión.");
                            return true;
                        }
                        if (project.getMembers() == null || (project.getMembers() != null && project.getAllMembers().size() < maxProjectMembers)) {
                            if (args.length >= 2) {
                                if (Bukkit.getOfflinePlayer(args[1]).isOnline()) {
                                    Player target = Bukkit.getPlayer(args[1]);
                                    project.addMember(target);
                                    project.upload();

                                    PlayerData playerData = new PlayerData(Bukkit.getOfflinePlayer(args[1]));

                                    playerData.addToList("projects", project.getId(), false);

                                    playerData.save();

                                    p.sendMessage(projectsPrefix + "Has agregado a §a" + new ServerPlayer(target).getName() + "§f al proyecto §a" + project.getName() + "§f.");
                                    target.sendMessage(projectsPrefix + "Has sido añadido al proyecto §a" + project.getName() + "§f.");

                                    new Country(project.getCountry()).getLogs().sendMessage(":pencil: **" + s.getName() + "** ha agregado a **" + new ServerPlayer(target).getName() + "** al proyecto `" + project.getId() + "`.").queue();
                                } else {
                                    p.sendMessage(projectsPrefix + "El jugador introducido no existe o no se encuentra online.");
                                }
                            } else {
                                p.sendMessage(projectsPrefix + "Introduce un jugador.");
                            }
                        } else {
                            p.sendMessage(projectsPrefix + "El proyecto ya alcanzó la capacidad máxima de miembros.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + "No eres el líder de este proyecto.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
                return true;
            }

            if (args[0].equals("remove") || args[0].equals("remover") || args[0].equals("quitar")) {
                if (!(p.hasPermission("bteconosur.projects.remove"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                Project project = null;
                try {
                    project = new Project(p.getLocation());
                    if (project.getOwner() == p) {
                        if (project.isPending()) {
                            p.sendMessage("No puedes hacer esto mientras el proyecto está pendiente de revisión.");
                            return true;
                        }
                        if (project.getMembers() != null) {
                            if (args.length >= 2) {
                                if (project.getMembers().contains(Bukkit.getOfflinePlayer(args[1]))) {
                                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                                    project.removeMember(target);
                                    project.upload();

                                    PlayerData playerData = new PlayerData(Bukkit.getOfflinePlayer(args[1]));

                                    playerData.removeFromList("projects", project.getId());

                                    playerData.save();

                                    p.sendMessage(projectsPrefix + "Has removido a §a" + new ServerPlayer(target).getName() + "§f del proyecto §a" + project.getName() + "§f.");

                                    new ServerPlayer(target).sendNotification(projectsPrefix + "Has sido removido del proyecto **§a" + project.getName(true) + "§f**.");

                                    new Country(project.getCountry()).getLogs().sendMessage(":pencil: **" + s.getName() + "** ha removido a **" + new ServerPlayer(target).getName() + "** del proyecto `" + project.getId() + "`.").queue();
                                } else {
                                    p.sendMessage(projectsPrefix + "El jugador introducido no es parte del proyecto.");
                                }
                            } else {
                                p.sendMessage(projectsPrefix + "Introduce un jugador.");
                            }
                        } else {
                            p.sendMessage(projectsPrefix + "El proyecto no tiene miembros.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + "No eres el líder de este proyecto.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
                return true;
            }

            if (args[0].equals("transfer") || args[0].equals("transferir")) {
                if (!(p.hasPermission("bteconosur.projects.transfer"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());
                    if (project.getOwner() == p) {
                        if (project.isPending()) {
                            p.sendMessage("No puedes hacer esto mientras el proyecto está pendiente de revisión.");
                            return true;
                        }
                        if (args.length >= 2) {
                            if (project.getMembers().contains(Bukkit.getOfflinePlayer(args[1]))) {
                                if (Bukkit.getOfflinePlayer(args[1]).isOnline()) {
                                    if (transferConfirmation.contains(p)) {
                                        transferConfirmation.remove(p);
                                        Player target = Bukkit.getPlayer(args[1]);

                                        if (new ServerPlayer(target).getOwnedProjects().size() > maxProjectsPerPlayer) {
                                            p.sendMessage(projectsPrefix + "El jugador introducido ya alcanzó su límite de proyectos.");
                                            return true;
                                        }

                                        project.addMember(p);
                                        project.removeMember(target);
                                        project.setOwner(target);
                                        project.upload();

                                        p.sendMessage(projectsPrefix + "Has transferido el proyecto §a" + project.getName() + " §fa §a" + target.getName() + "§f.");
                                        target.sendMessage(projectsPrefix + "§a" + p.getName() + " §fte ha transferido el proyecto §a" + project.getName() + "§f.");
                                    } else {
                                        transferConfirmation.add(p);
                                        p.sendMessage(projectsPrefix + "§cNo puedes deshacer esta acción. §fUsa el comando de nuevo para confirmar.");
                                    }
                                } else {
                                    p.sendMessage(projectsPrefix + "El jugador no se encuentra online.");
                                }
                            } else {
                                p.sendMessage(projectsPrefix + "El jugador no es un miembro de tu proyecto.");
                            }
                        } else {
                            p.sendMessage(projectsPrefix + "Introduce un jugador.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + "No eres el líder de este proyecto.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
            }

            if (args[0].equals("leave") || args[0].equals("abandonar")) {
                if (!(p.hasPermission("bteconosur.projects.leave"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());

                    if (project.isPending()) {
                        p.sendMessage("No puedes hacer esto mientras el proyecto está pendiente de revisión.");
                        return true;
                    }

                    if (project.getOwner() == p) {
                        if (leaveConfirmation.contains(p)) {
                            leaveConfirmation.remove(p);
                            project.empty();
                            project.setName(null);
                            project.upload();
                            p.sendMessage(projectsPrefix + "Has abandonado el proyecto §a" + project.getName() + "§f.");
                            PlayerData playerData = new PlayerData(p);
                            playerData.removeFromList("projects", project.getId());
                            playerData.save();
                            getLogsChannel(project.getCountry()).sendMessage(":outbox_tray: **" + s.getName() + "** ha abandonado el proyecto `" + project.getId() + "`.").queue();

                            if (project.getMembers() != null) {
                                for (OfflinePlayer member : project.getMembers()) {
                                    playerData = new PlayerData(member);
                                    playerData.removeFromList("projects", project.getId());
                                    playerData.save();

                                    new ServerPlayer(member).sendNotification("El líder de tu proyecto **§a" + project.getName(true) + "§f** ha abandonado el proyecto, por lo que tú también has salido.");
                                    getLogsChannel(project.getCountry()).sendMessage(":outbox_tray: **" + new ServerPlayer(member).getName() + "** ha abandonado el proyecto `" + project.getId() + "`.").queue();
                                }
                            }
                        } else {
                            leaveConfirmation.add(p);
                            p.sendMessage(projectsPrefix + "§cEsta acción no se puede deshacer. §fUsa el comando de nuevo para confirmar.");
                        }
                    } else if (project.getMembers() != null && project.getMembers().contains(p)) {
                        project.removeMember(p);
                        project.upload();

                        PlayerData playerData = new PlayerData(p);
                        playerData.removeFromList("projects", project.getId());
                        playerData.save();

                        p.sendMessage(projectsPrefix + "Has abandonado el proyecto §a" + project.getName() + "§f.");

                        new ServerPlayer(project.getOwner()).sendNotification(projectsPrefix + "**§a" + new ServerPlayer(p).getName() + "§f** ha abandonado tu proyecto **§a" + project.getName(true) + "§f**.");

                        getLogsChannel(project.getCountry()).sendMessage(":outbox_tray: **" + s.getName() + "** ha abandonado el proyecto `" + project.getId() + "`.").queue();
                    } else {
                        p.sendMessage(projectsPrefix + "No eres miembro de este proyecto.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                    e.printStackTrace();
                }
            }

            if (args[0].equals("borders") || args[0].equals("bordes")) {
                if (!(p.hasPermission("bteconosur.projects.showborder"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());

                    int maxY = p.getLocation().getBlockY() + 10;
                    int minY = p.getLocation().getBlockY() - 10;

                    Polygonal2DRegionSelector selector = new Polygonal2DRegionSelector((World) new BukkitWorld(mainWorld), project.getPoints(), minY, maxY);
                    methods.setSelection(p, selector);

                    p.sendMessage(projectsPrefix + "Mostrando los bordes del proyecto §a" + project.getName() + "§f. §7Requiere WorldEdit CUI.");
                } catch (Exception e) {
                    e.printStackTrace();
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
            }

            if (args[0].equals("review") || args[0].equals("revisar")) {
                if (!(p.hasPermission("bteconosur.projects.manage.review"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                List<String> permissionCountries = s.getPermissionCountries();

                try {
                    Project project = new Project(p.getLocation());

                    if (permissionCountries.contains(project.getCountry())) {

                        if (project.isPending()) {

                            if (args.length > 1) {
                                if (args[1].equals("accept") || args[1].equals("aceptar")) {
                                    // ADD POINTS

                                    Integer amount;

                                    if (project.getDifficulty().equals("dificil")) {
                                        amount = hardPoints;
                                    } else if (project.getDifficulty().equals("intermedio")) {
                                        amount = mediumPoints;
                                    } else {
                                        amount = easyPoints;
                                    }

                                    getLogsChannel(project.getCountry()).sendMessage(":mag: **" + s.getName() + "** ha aprobado el proyecto `" + project.getId() + "`.").queue();
                                    p.sendMessage(projectsPrefix + "Has aceptado el proyecto §a" + project.getId() + "§f.");


                                    ServerPlayer owner = new ServerPlayer(project.getOwner());

                                    owner.addPoints(new Country(project.getCountry()), amount);

                                    PlayerData playerData = new PlayerData(project.getOwner());
                                    Integer finishedProjects;
                                    if (playerData.getData("finished_projects_" + project.getCountry()) != null) {
                                        finishedProjects = (Integer) playerData.getData("finished_projects_" + project.getCountry());
                                        finishedProjects++;
                                    } else {
                                        finishedProjects = 1;
                                    }
                                    playerData.setData("finished_projects_" + project.getCountry(), finishedProjects);
                                    playerData.save();

                                    owner.sendNotification(projectsPrefix + "Tu proyecto **§a" + project.getName(true) + "§f** ha sido aceptado.");
                                    owner.sendNotification(pointsPrefix + "Has conseguido **§a" + amount + "§f** puntos. §7Total: " + owner.getPoints(new Country(project.getCountry())));

                                    if (project.getMembers() != null) {
                                        for (OfflinePlayer member : project.getMembers()) {
                                            ServerPlayer m = new ServerPlayer(member);
                                            m.addPoints(new Country(project.getCountry()), amount);

                                            PlayerData playerDataMember = new PlayerData(member);
                                            Integer finishedProjectsMember;
                                            if (playerDataMember.getData("finished_projects_" + project.getCountry()) != null) {
                                                finishedProjectsMember = (Integer) playerDataMember.getData("finished_projects_" + project.getCountry());
                                                finishedProjectsMember++;
                                            } else {
                                                finishedProjectsMember = 1;
                                            }
                                            playerDataMember.setData("finished_projects_" + project.getCountry(), finishedProjectsMember);
                                            playerDataMember.save();

                                            m.sendNotification(projectsPrefix + "Tu proyecto **§a" + project.getName(true) + "§f** ha sido aceptado.");
                                            m.sendNotification(pointsPrefix + "Has conseguido **§a" + amount + "§f** puntos. §7Total: " + m.getPoints(new Country(project.getCountry())));
                                        }
                                    }

                                    project.delete();

                                }
                                if (args[1].equals("continue") || args[1].equals("continuar")) {
                                    project.setPending(false);
                                    project.upload();

                                    p.sendMessage(projectsPrefix + "Has continuado el proyecto §a" + project.getId() + "§f.");

                                    if (project.getAllMembers() != null) {
                                        for (OfflinePlayer member : project.getAllMembers()) {
                                            new ServerPlayer(member).sendNotification(projectsPrefix + "Tu proyecto **§a" + project.getName() + "§f** ha sido continuado.");
                                        }
                                    }

                                    getLogsChannel(project.getCountry()).sendMessage(":mag: **" + s.getName() + "** ha continuado el proyecto `" + project.getId() + "`.").queue();
                                }
                                if (args[1].equals("deny") || args[1].equals("denegar") || args[1].equals("rechazar")) {
                                    project.setPending(false);
                                    project.empty();
                                    project.setName(null);
                                    project.upload();
                                    p.sendMessage(projectsPrefix + "Has rechazado el proyecto §a" + project.getId() + "§f.");

                                    if (project.getAllMembers() != null) {
                                        for (OfflinePlayer member : project.getAllMembers()) {
                                            new ServerPlayer(member).sendNotification(projectsPrefix + "Tu proyecto **§a" + project.getName(true) + "§f** ha sido rechazado.");
                                        }
                                    }


                                    getLogsChannel(project.getCountry()).sendMessage(":mag: **" + s.getName() + "** ha rechazado el proyecto `" + project.getId() + "`.").queue();
                                }
                            } else {
                                p.sendMessage(projectsPrefix + "Introduce una acción.");
                            }

                        } else {
                            p.sendMessage(projectsPrefix + "Este proyecto no esta pendiente de revisión.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + "No puedes hacer esto.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
            }

            if (args[0].equals("name") || args[0].equals("nombre")) {
                if (!(p.hasPermission("bteconosur.projects.name"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());
                    if (project.getOwner() == p) {
                        if (!(project.isPending())) {
                            if (args.length > 1 && args[1].matches("[a-zA-Z0-9_-]{1,32}")) {
                                project.setName(args[1]);
                                project.upload();

                                p.sendMessage(projectsPrefix + "Has cambiado el nombre del proyecto a §a" + project.getName() + "§f.");
                            } else {
                                p.sendMessage(projectsPrefix + "Introduce un nombre válido.");
                            }
                        } else {
                           p.sendMessage(projectsPrefix + "No puedes administrar tu proyecto mientras está pendiente.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + "No eres el líder de este proyecto.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de nigún proyecto.");
                }
            }

            if (args[0].equals("pending") || args[0].equals("pendientes")) {
                if (!(p.hasPermission("bteconosur.projects.manage.pending"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                List<String> pending = (List<String>) YamlManager.getYamlData(pluginFolder, "pending_projects/pending.yml").get("pending");
                if (pending != null) {
                    BookUtil.BookBuilder book = BookUtil.writtenBook();

                    List<BaseComponent[]> pages = new ArrayList<>();

                    List<List<String>> subLists= Lists.partition(pending, 12);

                    for (List<String> subList : subLists) {
                        BookUtil.PageBuilder page = new BookUtil.PageBuilder();
                        page.add("§7---[ §rPENDIENTES §7]---");
                        page.newLine();

                        for (String str : subList) {
                            try {
                                Project project = new Project(str);

                                String coord = project.getAverageCoordinate().getBlockX() + " " + new Coords2D(project.getAverageCoordinate()).getHighestY() + " " + project.getAverageCoordinate().getBlockZ();

                                page.add("- ");
                                page.add(BookUtil.TextBuilder.of(str)
                                        .onHover(BookUtil.HoverAction.showText("Click para ir"))
                                        .onClick(BookUtil.ClickAction.runCommand("/tp " + coord))
                                        .build());
                                page.newLine();

                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                        pages.add(page.build());
                    }

                    book.pages(pages);

                    BookUtil.openPlayer(p, book.build());
                } else {
                    p.sendMessage(projectsPrefix + "No hay proyectos pendientes de revisión.");
                }
            }

            if (args[0].equals("finish") || args[0].equals("terminar")|| args[0].equals("finalizar")) {
                if (!(p.hasPermission("bteconosur.projects.finish"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());

                    if (project.getOwner() == p) {
                        if (!(project.isPending())) {
                            if (finishConfirmation.contains(p)) {
                                finishConfirmation.remove(p);
                                project.setPending(true);
                                project.upload();

                                p.sendMessage(projectsPrefix + "Has marcado el proyecto §a" + project.getName() + "§f como terminado.");

                                if  (project.getMembers() != null) {
                                    for (OfflinePlayer member : project.getMembers()) {
                                        new ServerPlayer(member).sendNotification(projectsPrefix + "**§a" + new ServerPlayer(project.getOwner()).getName() + "§f** ha marcado el proyecto **§a" + project.getName(true) + "§f** como terminado.");
                                    }
                                }

                                getLogsChannel(project.getCountry()).sendMessage(":lock: **" + s.getName() + "** ha marcado el proyecto `" + project.getId() + "` como terminado.").queue();
                            } else {
                                finishConfirmation.add(p);
                                p.sendMessage(projectsPrefix + "§cNo podrás construir ni administrar tu proyecto mientras está en revisión. §fUsa el comando de nuevo para confirmar.");
                            }
                        } else {
                            p.sendMessage("Este proyecto ya está marcado como terminado.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + " No eres el líder de este proyecto.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
            }

            if (args[0].equals("info") || args[0].equals("informacion")) {
                if (!(p.hasPermission("bteconosur.projects.info"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());

                    //--------------------------------

                    BookUtil.BookBuilder builder = BookUtil.writtenBook();

                    BookUtil.PageBuilder page = new BookUtil.PageBuilder();

                    page.add("----[ PROYECTO ]----");

                    page.newLine();

                    if (project.getName() != project.getId()) {
                        page.add(BookUtil.TextBuilder.of("Nombre: ")
                                .color(ChatColor.GREEN)
                                .style(ChatColor.BOLD)
                                .build()
                        );
                        page.add(project.getName());
                        page.newLine();
                    }

                    page.add(BookUtil.TextBuilder.of("ID: ")
                            .color(ChatColor.GREEN)
                            .style(ChatColor.BOLD)
                            .build()
                    );
                    page.add(project.getId());
                    page.newLine();

                    page.add(BookUtil.TextBuilder.of("Dificultad: ")
                            .color(ChatColor.GREEN)
                            .style(ChatColor.BOLD)
                            .build()
                    );
                    page.add(project.getDifficulty().replace("facil", "Fácil").replace("intermedio", "Intermedio").replace("dificil", "Difícil"));
                    page.newLine();

                    page.add(BookUtil.TextBuilder.of("País: ")
                            .color(ChatColor.GREEN)
                            .style(ChatColor.BOLD)
                            .build()
                    );
                    page.add(StringUtils.capitalize(project.getCountry().replace("peru", "perú")));
                    page.newLine();

                    // TAG

                    if (project.getTag() != null) {
                        page.add(BookUtil.TextBuilder.of("Tipo: ")
                                .color(ChatColor.GREEN)
                                .style(ChatColor.BOLD)
                                .build()
                        );
                        page.add(StringUtils.capitalize(project.getTag().replace("_", " ")));
                        page.newLine();
                    }

                    // GMAPS

                    Coords2D gMaps = new Coords2D(project.getAverageCoordinate());
                    page.add(BookUtil.TextBuilder.of("GoogleMaps: ")
                            .color(ChatColor.GREEN)
                            .style(ChatColor.BOLD)
                            .build()
                    );
                    page.add(BookUtil.TextBuilder.of("ENLACE")
                            .color(ChatColor.BLACK)
                            .style(ChatColor.UNDERLINE)
                            .onHover(BookUtil.HoverAction.showText("Haz click para abrir el enlace."))
                            .onClick(BookUtil.ClickAction.openUrl("https://www.google.com/maps/@" + gMaps.getLat() + "," + gMaps.getLon() + ",19z"))
                            .build());
                    page.newLine();


                    if (project.getOwner() != null) {
                        page.add(BookUtil.TextBuilder.of("Líder: ")
                                .color(ChatColor.GREEN)
                                .style(ChatColor.BOLD)
                                .build()
                        );
                        page.add(BookUtil.TextBuilder.of(project.getOwner().getName())
                                .onHover(BookUtil.HoverAction.showText(new ServerPlayer(project.getOwner()).getLore()))
                                .build()
                        );
                        page.newLine();
                    }
                    
                    int i = 1;
                    if (project.getMembers() != null) {
                        page.add("§a§lMiembro(s): §r");
                        for (OfflinePlayer member : project.getMembers()) {
                            page.add(
                                    BookUtil.TextBuilder.of(new ServerPlayer(member).getName())
                                            .onHover(BookUtil.HoverAction.showText(new ServerPlayer(member).getLore()))
                                            .build()
                            );

                            if (i < project.getMembers().size()) {
                                page.add(", ");
                            }
                            i++;
                        }
                    }

                    builder.pages(page.build());

                    BookUtil.openPlayer(p, builder.build());

                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }

            }

            if (args[0].equals("list") || args[0].equals("lista")) {
                if (!(p.hasPermission("bteconosur.projects.list"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                if (s.getProjects().size() != 0) {
                    BookUtil.BookBuilder book = BookUtil.writtenBook();

                    List<BaseComponent[]> pages = new ArrayList<>();

                    for (Project project : s.getProjects()) {
                        BookUtil.PageBuilder page = new BookUtil.PageBuilder();

                        if (project.getName() != project.getId()) {
                            page.add("§a§lNombre: §r" + project.getName());
                            page.newLine();
                        }

                        page.add("§a§lID: §r" + project.getId());
                        page.newLine();

                        page.add("§a§lDificultad: §r" + project.getDifficulty().toUpperCase());
                        page.newLine();

                        page.add("§a§lPaís: §r" + StringUtils.capitalize(project.getCountry().replace("peru", "perú")));
                        page.newLine();

                        page.add("§a§lCoordenadas: §r\n");
                        page.add(
                                BookUtil.TextBuilder.of(project.getAverageCoordinate().getBlockX() + " " + new Coords2D(project.getAverageCoordinate()).getHighestY() + " " + project.getAverageCoordinate().getBlockZ())
                                        .onHover(BookUtil.HoverAction.showText("Click para ir"))
                                        .onClick(BookUtil.ClickAction.runCommand("/tp " + project.getAverageCoordinate().getBlockX() + " " + new Coords2D(project.getAverageCoordinate()).getHighestY() + " " + project.getAverageCoordinate().getBlockZ()))
                                        .build()
                        );
                        page.newLine();


                        page.add("§a§lLíder: §r");
                        page.add(
                                BookUtil.TextBuilder.of(new ServerPlayer(project.getOwner()).getName())
                                        .onHover(BookUtil.HoverAction.showText(new ServerPlayer(project.getOwner()).getLore()))
                                        .build()
                        );
                        page.newLine();

                        int i = 1;
                        if (project.getMembers() != null) {
                            page.add("§a§lMiembro(s): §r");
                            for (OfflinePlayer member : project.getMembers()) {
                                page.add(
                                        BookUtil.TextBuilder.of(new ServerPlayer(member).getName())
                                                .onHover(BookUtil.HoverAction.showText(new ServerPlayer(member).getLore()))
                                                .build()
                                );

                                if (i < project.getMembers().size()) {
                                    page.add(", ");
                                }
                                i++;
                            }
                        }

                        pages.add(page.build());
                    }

                    book.pages(pages);
                    BookUtil.openPlayer(p, book.build());
                } else {
                    p.sendMessage(projectsPrefix + "No tienes proyectos activos.");
                }
            }

            if (args[0].equals("members") || args[0].equals("miembros")) {
                if (!(p.hasPermission("bteconosur.projects.members"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }


            }

            if (args[0].equals("request") || args[0].equals("solicitar")) {
                if (!(p.hasPermission("bteconosur.projects.request"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());
                    if (project.getOwner() != null) {
                        if (!(project.getAllMembers().contains(p))) {
                            new ServerPlayer(project.getOwner()).sendNotification(projectsPrefix + "**§a" + new ServerPlayer(p).getName() + "§f** ha solicitado unirse a tu proyecto **§a" + project.getName(true) + "§f**.");
                        } else {
                            p.sendMessage(projectsPrefix + "Ya eres parte de este proyecto.");
                        }
                    } else {
                        p.sendMessage(projectsPrefix + "Este proyecto no está reclamado aún.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningun proyecto.");
                }
            }

            if (args[0].equals("tutorial")) {
                if (!(p.hasPermission("bteconosur.projects.tutorial"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }
            }

            if (args[0].equals("redefine") || args[0].equals("redefinir")) {
                if (!(p.hasPermission("bteconosur.projects.redefine"))) {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    return true;
                }

                try {
                    Project project = new Project(p.getLocation());

                    Region region = null;
                    try {
                        region = getSelection(p);
                    } catch (IncompleteRegionException e) {
                        p.sendMessage(projectsPrefix + "§cSelecciona un área primero.");
                    }

                    // GET POINTS

                    List<BlockVector2D> points = new ArrayList<>();

                    if (region instanceof CuboidRegion) {
                        CuboidRegion cuboidRegion = (CuboidRegion) region;
                        Vector first = cuboidRegion.getPos1();
                        Vector second = cuboidRegion.getPos2();

                        points.add(new BlockVector2D(first.getX(), first.getZ()));
                        points.add(new BlockVector2D(second.getX(), first.getZ()));
                        points.add(new BlockVector2D(second.getX(), second.getZ()));
                        points.add(new BlockVector2D(first.getX(), second.getZ()));
                    } else if (region instanceof Polygonal2DRegion) {
                        points = ((Polygonal2DRegion) region).getPoints();
                    } else {
                        p.sendMessage(projectsPrefix + "Debes seleccionar una region cúbica o poligonal.");
                        return true;
                    }

                    if (points.size() < 3) {
                        p.sendMessage(projectsPrefix + "Selecciona un área primero.");
                        return true;
                    }

                    if (points.size() > maxProjectPoints) {
                        p.sendMessage(projectsPrefix + "La selección no puede tener más de 15 puntos.");
                        return true;
                    }

                    Country country = new Country(points.get(0));

                    if (p.hasPermission("bteconosur.projects.manage.redefine")) {

                        if (!s.getPermissionCountries().contains(project.getCountry())) {
                            p.sendMessage(projectsPrefix + "No puedes hacer esto aquí.");
                        }

                        if (project.isPending()) {
                            p.sendMessage("No puedes hacer esto mientras el proyecto está pendiente de revisión.");
                            return true;
                        }

                        if (args.length < 2) {
                            p.sendMessage(projectsPrefix + "Introduce una dificultad, puede ser §afacil§f, §aintermedio§f o §adificil§f.");
                            return true;
                        }

                        if ((!(args[1].equals("facil"))) && (!(args[1].equals("intermedio"))) && (!(args[1].equals("dificil")))) {
                            p.sendMessage(projectsPrefix + "Introduce una dificultad válida, puede ser §afacil§f, §aintermedio§f o §adificil§f.");
                            return true;
                        }

                        project.setDifficulty(args[1]);
                        project.setPoints(points);

                        project.upload();
                        // SEND MESSAGES

                        p.sendMessage(projectsPrefix + "Proyecto con la ID §a" + project.getId() + "§f redefinido con dificultad §a" + project.getDifficulty().toUpperCase()  + "§f.");

                        String dscMessage = ":clipboard: **" + p.getName() + "** ha redefinido el proyecto `" + project.getId() + "` con dificultad `" + args[1].toUpperCase() + "` en las coordenadas: \n";
                        for (BlockVector2D point : project.getPoints()) {
                            dscMessage = dscMessage + "> " + Math.floor(point.getX()) + " " + Math.floor(p.getWorld().getHighestBlockAt(point.getBlockX(), point.getBlockZ()).getY()) + " " + Math.floor(point.getZ()) + "\n";
                        }
                        dscMessage = dscMessage.replace(".0", "");

                        String notif = "Tu proyecto **§a" + project.getName(true) + "§f** ha sido redefinido con dificultad **§a" + project.getDifficulty().toUpperCase() + "§f** en las coordenadas: \n";
                        for (BlockVector2D point : project.getPoints()) {
                            notif = notif + "> " + Math.floor(point.getX()) + " " + Math.floor(p.getWorld().getHighestBlockAt(point.getBlockX(), point.getBlockZ()).getY()) + " " + Math.floor(point.getZ()) + "\n";
                        }

                        if (project.getOwner() != null) {
                            new ServerPlayer(project.getOwner()).sendNotification(notif);
                        }

                        getLogsChannel(project.getCountry()).sendMessage(dscMessage).queue();
                        return true;
                    } else if (p.hasPermission("bteconosur.projects.redefine")) {

                        if (project.getOwner() != p) {
                            p.sendMessage(projectsPrefix + "No eres el líder de este proyecto.");
                            return true;
                        }

                        if (project.isPending()) {
                            p.sendMessage("No puedes hacer esto mientras el proyecto está pendiente de revisión.");
                            return true;
                        }

                        String channelId;
                        if (project.getCountry().equals("argentina")) {
                            channelId = "932074847016718426";
                        } else if (project.getCountry().equals("bolivia")) {
                            channelId = "932074847016718426";
                        } else if (project.getCountry().equals("chile")) {
                            channelId = "932074847016718426";
                        } else if (project.getCountry().equals("peru")) {
                            channelId = "932074847016718426";
                        } else {
                            p.sendMessage(projectsPrefix + "Los proyectos no funcionan aquí.");
                            return true;
                        }

                        EmbedBuilder request = new EmbedBuilder();
                        request.setColor(new Color(0, 255, 42));
                        request.setTitle(new ServerPlayer(p).getName() + " quiere redefinir el proyecto " + project.getId().toUpperCase() + ".");

                        List<String> oldCoords = new ArrayList<>();
                        for (BlockVector2D point : project.getPoints()) {
                            oldCoords.add(("> " + point.getX() + " " + new Coords2D(point).getHighestY() + " " + point.getZ()).replace(".0", ""));
                        }
                        request.addField(":blue_circle: Coordenadas antiguas:", String.join("\n", oldCoords), false);

                        List<String> newCoords = new ArrayList<>();
                        for (BlockVector2D point : points) {
                            newCoords.add(("> " + point.getX() + " " + new Coords2D(point).getHighestY() + " " + point.getZ()).replace(".0", ""));
                        }
                        request.addField(":red_circle: Coordenadas nuevas:", String.join("\n", newCoords), false);

                        // GMAPS

                        Double minX = project.getPoints().get(0).getX();
                        Double maxX = project.getPoints().get(0).getX();
                        Double minZ = project.getPoints().get(0).getZ();
                        Double maxZ = project.getPoints().get(0).getZ();

                        for (BlockVector2D point : points) {
                            if (point.getX() > maxX) {
                                maxX = point.getX();
                            }
                            if (point.getX() < minX) {
                                minX = point.getX();
                            }
                            if (point.getZ() > maxZ) {
                                maxZ = point.getZ();
                            }
                            if (point.getZ() < minZ) {
                                minZ = point.getZ();
                            }
                        }

                        Coords2D geoCoord = new Coords2D(new Location(mainWorld, (minX + maxX) / 2, 100, (minZ + maxZ) / 2));

                        request.addField(":map: Google Maps:", "https://www.google.com/maps/@" + geoCoord.getLat() + "," + geoCoord.getLon() + ",19z", false);

                        // IMAGE

                        String url;

                        List<String> coordsOld = new ArrayList<>();
                        for (BlockVector2D point : project.getPoints()) {
                            coordsOld.add(new Coords2D(point).getLat() + "," + new Coords2D(point).getLon());
                        }
                        coordsOld.add(new Coords2D(project.getPoints().get(0)).getLat() + "," + new Coords2D(project.getPoints().get(0)).getLon());

                        List<String> coordsNew = new ArrayList<>();
                        for (BlockVector2D point : points) {
                            coordsNew.add(new Coords2D(point).getLat() + "," + new Coords2D(point).getLon());
                        }
                        coordsNew.add(new Coords2D(points.get(0)).getLat() + "," + new Coords2D(points.get(0)).getLon());

                        url = "https://open.mapquestapi.com/staticmap/v5/map?key=" + key + "&type=sat&shape=" + String.join("|", coordsOld) + "|fill:6382DC50&shape=" + String.join("|", coordsNew) + "|fill:ff000050|border:ff0000&size=1280,720&imagetype=png";

                        request.setImage(url);

                        ActionRow actionRow = ActionRow.of(
                                Button.of(ButtonStyle.SECONDARY, "facil", "Fácil", Emoji.fromMarkdown("\uD83D\uDFE2")),
                                Button.of(ButtonStyle.SECONDARY, "intermedio", "Intermedio", Emoji.fromMarkdown("\uD83D\uDFE1")),
                                Button.of(ButtonStyle.SECONDARY, "dificil", "Difícil", Emoji.fromMarkdown("\uD83D\uDD34")),
                                Button.of(ButtonStyle.DANGER, "rechazar", "Rechazar", Emoji.fromMarkdown("✖️"))
                        );

                        MessageBuilder message = new MessageBuilder();
                        message.setEmbeds(request.build());
                        message.setActionRows(actionRow);

                        conoSurBot.getTextChannelById(channelId).sendMessage(message.build()).queue();

                        p.sendMessage(projectsPrefix + "Se ha enviado una solicitud para redefinir tu proyecto.");
                    } else {
                        p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                    }
                } catch (Exception e) {
                    p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                }
            }

            if (args[0].equals("tag") || args[0].equals("etiqueta")) {
                if (p.hasPermission("bteconosur.projects.manage.tag")) {
                    try {
                        Project project = new Project(p.getLocation());

                        if (!(s.getPermissionCountries().contains(project.getCountry()))) {
                            p.sendMessage(projectsPrefix + "No puedes hacer esto aquí.");
                        }

                        if (args.length > 1) {
                            if (args[1].equals("edificios") || args[1].equals("departamentos") || args[1].equals("casas") || args[1].equals("parques") || args[1].equals("establecimientos") || args[1].equals("carreteras") || args[1].equals("centros_comerciales")) {
                                project.setTag(args[1]);
                                project.upload();

                                new Country(project.getCountry()).getLogs().sendMessage(":label: **" + s.getName() + "** ha establecido la etiqueta del proyecto `" + project.getId() + "` en **" + args[1].replace("_", " ").toUpperCase() + "**.").queue();

                                p.sendMessage(projectsPrefix + "Has establecido la etiquteda del proyecto §a" + project.getId() + "§f en §a" + args[1].replace("_", " ").toUpperCase() + "§f.");
                            } else if (args[1].equals("delete")) {
                                project.setTag(null);
                                project.upload();

                                new Country(project.getCountry()).getLogs().sendMessage(":label: **" + s.getName() + "** ha eliminado la etiqueta del proyecto `" + project.getId() + "`.").queue();

                                p.sendMessage(projectsPrefix + "Has eliminado la etiqueta del proyecto §a" + project.getId() + "§f.");

                            } else {
                                p.sendMessage(projectsPrefix + "Introduce una etiqueta válida.");
                            }
                        } else {
                            p.sendMessage(projectsPrefix + "Introduce una etiqueta.");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        p.sendMessage(projectsPrefix + "No estás dentro de ningún proyecto.");
                    }
                } else {
                    p.sendMessage(projectsPrefix + "§cNo tienes permiso para hacer eso.");
                }
            }

            if (args[0].equals("find") || args[0].equals("encontrar")) {
                Inventory pRandomGui = Bukkit.createInventory(null, 27, "1. Elige una dificultad");

                ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
                ItemMeta gMeta = glass.getItemMeta();
                gMeta.setDisplayName(" ");
                glass.setItemMeta(gMeta);

                for (int i=0; i < 27; i++) {
                    pRandomGui.setItem(i, background);
                }

                pRandomGui.setItem(11, getCustomHead("§aFácil §f- 15 puntos", "§fProyectos de un área pequeña, con construcciones simples y rápidas.", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmFlMTE5YTIzODJlZGE4NjRiMjQ0ZmE4YzUzYWMzZTU0NDE2MzEwM2VlNjY3OTVmMGNkNmM2NGY3YWJiOGNmMSJ9fX0="));
                pRandomGui.setItem(13, getCustomHead("§eIntermedio §f- 50 puntos", "§fProyectos con una dificultad intermedia, que requieren un cierto nivel de planeación y dedicación.", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjIxOTk5NDM3YjFkZTJjNDcyMGFhZDQzMmIyZWE1MzgyZTE1NDgyNzc1MjNmMjViMGY1NWMzOWEwMWMyYTljMCJ9fX0="));
                pRandomGui.setItem(15, getCustomHead("§cDifícil §f- 100 puntos", "§fProyectos de gran tamaño y/o dificultad, que requieren gran detalle y planificación previa.", "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjQ1ZTg1ZWRkYTFhODFkMjI0YWRiNzEzYjEzYjcwMzhkNWNjNmJlY2Q5OGE3MTZiOGEzZGVjN2UzYTBmOTgxNyJ9fX0="));

                p.openInventory(pRandomGui);
            }
        }
        return true;
    }
}
