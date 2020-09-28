package net.minecraft.client.network;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.status.IClientStatusNetHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.status.client.CPingPacket;
import net.minecraft.network.status.client.CServerQueryPacket;
import net.minecraft.network.status.server.SPongPacket;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ServerPinger {
   private static final Splitter PING_RESPONSE_SPLITTER = Splitter.on('\u0000').limit(6);
   private static final Logger LOGGER = LogManager.getLogger();
   /** A list of NetworkManagers that have pending pings */
   private final List<NetworkManager> pingDestinations = Collections.synchronizedList(Lists.newArrayList());

   public void ping(final ServerData server) throws UnknownHostException {
      ServerAddress serveraddress = ServerAddress.fromString(server.serverIP);
      final NetworkManager networkmanager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(serveraddress.getIP()), serveraddress.getPort(), false);
      this.pingDestinations.add(networkmanager);
      server.serverMOTD = I18n.format("multiplayer.status.pinging");
      server.pingToServer = -1L;
      server.playerList = null;
      networkmanager.setNetHandler(new IClientStatusNetHandler() {
         private boolean successful;
         private boolean receivedStatus;
         private long pingSentAt;

         public void handleServerInfo(SServerInfoPacket packetIn) {
            if (this.receivedStatus) {
               networkmanager.closeChannel(new TranslationTextComponent("multiplayer.status.unrequested"));
            } else {
               this.receivedStatus = true;
               ServerStatusResponse serverstatusresponse = packetIn.getResponse();
               if (serverstatusresponse.getServerDescription() != null) {
                  server.serverMOTD = serverstatusresponse.getServerDescription().getFormattedText();
               } else {
                  server.serverMOTD = "";
               }

               if (serverstatusresponse.getVersion() != null) {
                  server.gameVersion = serverstatusresponse.getVersion().getName();
                  server.version = serverstatusresponse.getVersion().getProtocol();
               } else {
                  server.gameVersion = I18n.format("multiplayer.status.old");
                  server.version = 0;
               }

               if (serverstatusresponse.getPlayers() != null) {
                  server.populationInfo = TextFormatting.GRAY + "" + serverstatusresponse.getPlayers().getOnlinePlayerCount() + "" + TextFormatting.DARK_GRAY + "/" + TextFormatting.GRAY + serverstatusresponse.getPlayers().getMaxPlayers();
                  if (ArrayUtils.isNotEmpty(serverstatusresponse.getPlayers().getPlayers())) {
                     StringBuilder stringbuilder = new StringBuilder();

                     for(GameProfile gameprofile : serverstatusresponse.getPlayers().getPlayers()) {
                        if (stringbuilder.length() > 0) {
                           stringbuilder.append("\n");
                        }

                        stringbuilder.append(gameprofile.getName());
                     }

                     if (serverstatusresponse.getPlayers().getPlayers().length < serverstatusresponse.getPlayers().getOnlinePlayerCount()) {
                        if (stringbuilder.length() > 0) {
                           stringbuilder.append("\n");
                        }

                        stringbuilder.append(I18n.format("multiplayer.status.and_more", serverstatusresponse.getPlayers().getOnlinePlayerCount() - serverstatusresponse.getPlayers().getPlayers().length));
                     }

                     server.playerList = stringbuilder.toString();
                  }
               } else {
                  server.populationInfo = TextFormatting.DARK_GRAY + I18n.format("multiplayer.status.unknown");
               }

               if (serverstatusresponse.getFavicon() != null) {
                  String s = serverstatusresponse.getFavicon();
                  if (s.startsWith("data:image/png;base64,")) {
                     server.setBase64EncodedIconData(s.substring("data:image/png;base64,".length()));
                  } else {
                     ServerPinger.LOGGER.error("Invalid server icon (unknown format)");
                  }
               } else {
                  server.setBase64EncodedIconData((String)null);
               }

               net.minecraftforge.fml.client.ClientHooks.processForgeListPingData(serverstatusresponse, server);

               this.pingSentAt = Util.milliTime();
               networkmanager.sendPacket(new CPingPacket(this.pingSentAt));
               this.successful = true;
            }
         }

         public void handlePong(SPongPacket packetIn) {
            long i = this.pingSentAt;
            long j = Util.milliTime();
            server.pingToServer = j - i;
            networkmanager.closeChannel(new TranslationTextComponent("multiplayer.status.finished"));
         }

         /**
          * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
          */
         public void onDisconnect(ITextComponent reason) {
            if (!this.successful) {
               ServerPinger.LOGGER.error("Can't ping {}: {}", server.serverIP, reason.getString());
               server.serverMOTD = TextFormatting.DARK_RED + I18n.format("multiplayer.status.cannot_connect");
               server.populationInfo = "";
               ServerPinger.this.tryCompatibilityPing(server);
            }

         }

         /**
          * Returns this the NetworkManager instance registered with this NetworkHandlerPlayClient
          */
         public NetworkManager getNetworkManager() {
            return networkmanager;
         }
      });

      try {
         networkmanager.sendPacket(new CHandshakePacket(serveraddress.getIP(), serveraddress.getPort(), ProtocolType.STATUS));
         networkmanager.sendPacket(new CServerQueryPacket());
      } catch (Throwable throwable) {
         LOGGER.error(throwable);
      }

   }

   private void tryCompatibilityPing(final ServerData server) {
      final ServerAddress serveraddress = ServerAddress.fromString(server.serverIP);
      (new Bootstrap()).group(NetworkManager.CLIENT_NIO_EVENTLOOP.getValue()).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel p_initChannel_1_) throws Exception {
            try {
               p_initChannel_1_.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException var3) {
               ;
            }

            p_initChannel_1_.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
               public void channelActive(ChannelHandlerContext p_channelActive_1_) throws Exception {
                  super.channelActive(p_channelActive_1_);
                  ByteBuf bytebuf = Unpooled.buffer();

                  try {
                     bytebuf.writeByte(254);
                     bytebuf.writeByte(1);
                     bytebuf.writeByte(250);
                     char[] achar = "MC|PingHost".toCharArray();
                     bytebuf.writeShort(achar.length);

                     for(char c0 : achar) {
                        bytebuf.writeChar(c0);
                     }

                     bytebuf.writeShort(7 + 2 * serveraddress.getIP().length());
                     bytebuf.writeByte(127);
                     achar = serveraddress.getIP().toCharArray();
                     bytebuf.writeShort(achar.length);

                     for(char c1 : achar) {
                        bytebuf.writeChar(c1);
                     }

                     bytebuf.writeInt(serveraddress.getPort());
                     p_channelActive_1_.channel().writeAndFlush(bytebuf).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                  } finally {
                     bytebuf.release();
                  }

               }

               protected void channelRead0(ChannelHandlerContext p_channelRead0_1_, ByteBuf p_channelRead0_2_) throws Exception {
                  short short1 = p_channelRead0_2_.readUnsignedByte();
                  if (short1 == 255) {
                     String s = new String(p_channelRead0_2_.readBytes(p_channelRead0_2_.readShort() * 2).array(), StandardCharsets.UTF_16BE);
                     String[] astring = Iterables.toArray(ServerPinger.PING_RESPONSE_SPLITTER.split(s), String.class);
                     if ("\u00a71".equals(astring[0])) {
                        int i = MathHelper.getInt(astring[1], 0);
                        String s1 = astring[2];
                        String s2 = astring[3];
                        int j = MathHelper.getInt(astring[4], -1);
                        int k = MathHelper.getInt(astring[5], -1);
                        server.version = -1;
                        server.gameVersion = s1;
                        server.serverMOTD = s2;
                        server.populationInfo = TextFormatting.GRAY + "" + j + "" + TextFormatting.DARK_GRAY + "/" + TextFormatting.GRAY + k;
                     }
                  }

                  p_channelRead0_1_.close();
               }

               public void exceptionCaught(ChannelHandlerContext p_exceptionCaught_1_, Throwable p_exceptionCaught_2_) throws Exception {
                  p_exceptionCaught_1_.close();
               }
            });
         }
      }).channel(NioSocketChannel.class).connect(serveraddress.getIP(), serveraddress.getPort());
   }

   public void pingPendingNetworks() {
      synchronized(this.pingDestinations) {
         Iterator<NetworkManager> iterator = this.pingDestinations.iterator();

         while(iterator.hasNext()) {
            NetworkManager networkmanager = iterator.next();
            if (networkmanager.isChannelOpen()) {
               networkmanager.tick();
            } else {
               iterator.remove();
               networkmanager.handleDisconnection();
            }
         }

      }
   }

   public void clearPendingNetworks() {
      synchronized(this.pingDestinations) {
         Iterator<NetworkManager> iterator = this.pingDestinations.iterator();

         while(iterator.hasNext()) {
            NetworkManager networkmanager = iterator.next();
            if (networkmanager.isChannelOpen()) {
               iterator.remove();
               networkmanager.closeChannel(new TranslationTextComponent("multiplayer.status.cancelled"));
            }
         }

      }
   }
}