import { Directive, ElementRef, HostListener, inject } from '@angular/core';

@Directive({
  selector: 'img[hotelImageFallback]',
  standalone: true,
})
export class HotelImageFallbackDirective {
  private readonly image = inject(ElementRef<HTMLImageElement>).nativeElement;
  private fallbackShown = false;

  @HostListener('error')
  useFallback(): void {
    if (this.fallbackShown) return;
    this.fallbackShown = true;
    this.image.src = '/hotel-placeholder.svg';
  }
}
