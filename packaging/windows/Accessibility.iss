[Code]
{ Adaptación local: conservar la plantilla visual compartida y la navegación nativa. }
procedure LyraAccessibleKeyDown(Sender: TObject; var Key: Word; Shift: TShiftState);
begin
  if (WizardForm.CurPageID = ModernPage.ID) and (Key = 13) and ModernPath.Focused then
  begin
    Key := 0;
    ModernStartInstall(Sender);
  end;
  { Tab y Shift+Tab los gestiona Windows, incluyendo el campo de ruta. }
end;

procedure InitializeAccessibleControls();
var LabelControl: TNewStaticText;
begin
  ModernPath.ReadOnly := False;
  ModernPath.TabStop := True;
  ModernPath.OnKeyDown := nil;
  WizardForm.OnKeyDown := @LyraAccessibleKeyDown;
  LabelControl := TNewStaticText.Create(WizardForm);
  LabelControl.Parent := ModernHost;
  LabelControl.SetBounds(ScaleX(32), ScaleY(274), ScaleX(464), ScaleY(20));
  LabelControl.Caption := '&Ubicación de instalación';
  LabelControl.FocusControl := ModernPath;
  LabelControl.Font.Assign(ModernPath.Font);
  LabelControl.Font.Color := ModernMuted;
  LabelControl.Color := ModernSurface;
  LabelControl.StyleElements := [];
end;
