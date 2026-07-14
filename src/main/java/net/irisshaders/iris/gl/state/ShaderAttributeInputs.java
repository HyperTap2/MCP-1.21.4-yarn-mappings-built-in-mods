package net.irisshaders.iris.gl.state;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

public class ShaderAttributeInputs {
   private boolean ie;
   private boolean color;
   private boolean tex;
   private boolean overlay;
   private boolean light;
   private boolean normal;
   private boolean newLines;
   private boolean glint;
   private boolean text;

   public ShaderAttributeInputs(VertexFormat format, boolean isFullbright, boolean isLines, boolean glint, boolean text, boolean ie) {
      if (format == VertexFormats.LINES && !isLines) {
         this.newLines = true;
      }

      this.ie = ie;
      this.text = text;
      this.glint = glint;
      format.getAttributeNames().forEach(name -> {
         if ("Color".equals(name)) {
            this.color = true;
         }

         if ("UV0".equals(name)) {
            this.tex = true;
         }

         if ("UV1".equals(name)) {
            this.overlay = true;
         }

         if ("UV2".equals(name) && !isFullbright) {
            this.light = true;
         }

         if ("Normal".equals(name)) {
            this.normal = true;
         }
      });
   }

   public ShaderAttributeInputs(boolean color, boolean tex, boolean overlay, boolean light, boolean normal) {
      this.color = color;
      this.tex = tex;
      this.overlay = overlay;
      this.light = light;
      this.normal = normal;
   }

   public boolean hasColor() {
      return this.color;
   }

   public boolean hasTex() {
      return this.tex;
   }

   public boolean hasOverlay() {
      return this.overlay;
   }

   public boolean hasLight() {
      return this.light;
   }

   public boolean hasNormal() {
      return this.normal;
   }

   public boolean isNewLines() {
      return this.newLines;
   }

   public boolean isGlint() {
      return this.glint;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.color ? 1231 : 1237);
      result = 31 * result + (this.tex ? 1231 : 1237);
      result = 31 * result + (this.overlay ? 1231 : 1237);
      result = 31 * result + (this.light ? 1231 : 1237);
      result = 31 * result + (this.normal ? 1231 : 1237);
      result = 31 * result + (this.newLines ? 1231 : 1237);
      result = 31 * result + (this.glint ? 1231 : 1237);
      return 31 * result + (this.text ? 1231 : 1237);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      } else if (obj == null) {
         return false;
      } else if (this.getClass() != obj.getClass()) {
         return false;
      } else {
         ShaderAttributeInputs other = (ShaderAttributeInputs)obj;
         if (this.color != other.color) {
            return false;
         } else if (this.tex != other.tex) {
            return false;
         } else if (this.overlay != other.overlay) {
            return false;
         } else if (this.light != other.light) {
            return false;
         } else if (this.normal != other.normal) {
            return false;
         } else if (this.newLines != other.newLines) {
            return false;
         } else {
            return this.glint != other.glint ? false : this.text == other.text;
         }
      }
   }

   public boolean isText() {
      return this.text;
   }

   public boolean isIE() {
      return this.ie;
   }
}
