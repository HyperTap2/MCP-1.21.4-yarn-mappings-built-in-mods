package com.viaversion.viafabricplus.screen.impl;

import com.viaversion.viafabricplus.ViaFabricPlusImpl;
import com.viaversion.viafabricplus.screen.VFPScreen;
import com.viaversion.viaversion.util.DumpUtil;
import java.io.File;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

public final class ReportIssuesScreen extends VFPScreen {
   public static final ReportIssuesScreen INSTANCE = new ReportIssuesScreen();
   private final Map<String, Runnable> actions = new LinkedHashMap<>();
   private long delay = -1L;

   public ReportIssuesScreen() {
      super(Text.translatable("screen.viafabricplus.report_issues"), true);
      if (this.actions.isEmpty()) {
         this.actions
            .put(
               "report.viafabricplus.bug_report",
               () -> {
                  Util.getOperatingSystem()
                     .open(URI.create("https://github.com/ViaVersion/ViaFabricPlus/issues/new?assignees=&labels=bug&projects=&template=bug_report.yml"));
                  this.setupSubtitle(Text.translatable("report.viafabricplus.bug_report.response"));
               }
            );
         this.actions
            .put(
               "report.viafabricplus.feature_request",
               () -> {
                  Util.getOperatingSystem()
                     .open(
                        URI.create(
                           "https://github.com/ViaVersion/ViaFabricPlus/issues/new?assignees=&labels=enhancement&projects=&template=feature_request.yml"
                        )
                     );
                  this.setupSubtitle(Text.translatable("report.viafabricplus.feature_request.response"));
               }
            );
         this.actions
            .put("report.viafabricplus.create_via_dump", () -> DumpUtil.postDump(this.client.getSession().getUuidOrNull()).whenComplete((s, throwable) -> {
               if (throwable != null) {
                  this.setupSubtitle(Text.translatable("report.viafabricplus.create_via_dump.failed"));
                  ViaFabricPlusImpl.INSTANCE.logger().error("Failed to create a dump", throwable);
               } else {
                  this.setupSubtitle(Text.translatable("report.viafabricplus.create_via_dump.success"));
                  this.client.keyboard.setClipboard(s);
               }
            }));
         this.actions.put("report.viafabricplus.open_logs", () -> {
            Util.getOperatingSystem().open(new File(this.client.runDirectory, "logs"));
            this.setupSubtitle(Text.translatable("report.viafabricplus.open_logs.response"));
         });
      }
   }

   @Override
   protected void init() {
      super.init();
      this.setupDefaultSubtitle();
      int i = 0;

      for (Entry<String, Runnable> entry : this.actions.entrySet()) {
         this.addDrawableChild(
            ButtonWidget.builder(Text.translatable(entry.getKey()), button -> entry.getValue().run())
               .position(this.width / 2 - 100, this.height / 2 - 25 + i * 23)
               .size(200, 20)
               .build()
         );
         i++;
      }
   }

   @Override
   public void setupSubtitle(@Nullable Text subtitle) {
      super.setupSubtitle(subtitle);
      this.delay = System.currentTimeMillis();
   }

   public void tick() {
      super.tick();
      if (this.delay != -1L && System.currentTimeMillis() - this.delay > 5000L) {
         this.setupDefaultSubtitle();
         this.delay = -1L;
      }
   }
}
