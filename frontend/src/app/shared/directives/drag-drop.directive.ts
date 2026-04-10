import { Directive, HostListener, output, signal } from '@angular/core';

@Directive({
  selector: '[appDragDrop]',
  standalone: true,
  exportAs: 'dragDrop',
})
export class DragDropDirective {
  readonly fileDropped = output<File[]>();

  private readonly _isDragging = signal(false);
  readonly isDragging = this._isDragging.asReadonly();

  private dragCounter = 0;

  @HostListener('dragenter', ['$event'])
  onDragEnter(event: DragEvent): void {
    event.preventDefault();
    this.dragCounter++;
    this._isDragging.set(true);
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragCounter--;
    if (this.dragCounter === 0) {
      this._isDragging.set(false);
    }
  }

  @HostListener('dragover', ['$event'])
  onDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  @HostListener('drop', ['$event'])
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragCounter = 0;
    this._isDragging.set(false);
    const files = Array.from(event.dataTransfer?.files ?? []);
    if (files.length > 0) {
      this.fileDropped.emit(files);
    }
  }
}
